package fr.caraito.fastLogIp;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main extends JavaPlugin implements Listener {

    private FileConfiguration data;
    private File dataFile;
    public static final Map<String, Boolean> isLogged = new HashMap<>();
    public static final Map<String, Long> loginTimeout = new HashMap<>();
    public static final Map<String, Boolean> isInventoryEmpty = new HashMap<>();
    private static Main instance;

    @Override
    public void onEnable() {

        instance = this;

        getDataFolder().mkdirs();
        loadData();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("login").setExecutor(this);
        getCommand("regentoken").setExecutor(new RegenToken(this));
        getCommand("adminregentoken").setExecutor(new AdminRegenToken(this));
        new LoginChecker().runTaskTimer(this, 20L, 20L);

    }

    public void loadData() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            if (!dataFile.exists()) {
                try {
                    dataFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String generateToken(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
        Random random = new Random();
        StringBuilder token = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            token.append(chars.charAt(index));
        }

        return token.toString();
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String currentIp = p.getAddress().getAddress().getHostAddress();

        Inventory inventory = p.getInventory();

        if (!isInventoryCompletelyEmpty(p)) {
            saveInventory(p);
            p.getInventory().clear();
        }


        if (!data.contains("players." + p.getName().toLowerCase())) {
            String token = generateToken(8);
            data.set("players." + p.getName().toLowerCase() + ".ip", currentIp);
            data.set("players." + p.getName().toLowerCase() + ".token", token);
            saveData();
            isLogged.put(p.getName().toLowerCase(), true);
            p.sendMessage(ChatColor.GREEN + "Bienvenue, vous êtes automatiquement connecté.");
            p.sendMessage(ChatColor.YELLOW + "Voici votre token : §e" + token);
            p.sendMessage(ChatColor.GRAY + "Gardez-le précieusement !");
            p.sendMessage(ChatColor.GRAY + "Il permettra de vous reconnecter");
        } else {
            String savedIp = data.getString("players." + p.getName().toLowerCase() + ".ip");
            if (currentIp.equals(savedIp)) {
                isLogged.put(p.getName().toLowerCase(), true);
                p.sendMessage(ChatColor.GREEN + "Connexion automatique réussie.");
                restoreInventory(p);
            } else {
                isLogged.put(p.getName().toLowerCase(), false);
                p.sendMessage(ChatColor.RED + "Veuillez vous connecter avec /login <token>");
                loginTimeout.put(p.getName().toLowerCase(), System.currentTimeMillis());


            }
        }
    }


    private void saveInventory(Player p) {
        File invFile = new File(getDataFolder(), "inventories.yml");
        FileConfiguration invCfg = YamlConfiguration.loadConfiguration(invFile);

        ItemStack[] contents = p.getInventory().getContents();
        invCfg.set("inventory." + p.getName().toLowerCase(), Arrays.asList(contents));

        try {
            invCfg.save(invFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isInventoryCompletelyEmpty(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        if (player.getInventory().getItemInOffHand().getType() != Material.AIR) {
            return false;
        }
        return true;
    }





    private void restoreInventory(Player p) {
        File invFile = new File(getDataFolder(), "inventories.yml");
        if (!invFile.exists()) return;

        FileConfiguration invCfg = YamlConfiguration.loadConfiguration(invFile);
        List<?> rawList = invCfg.getList("inventory." + p.getName().toLowerCase());

        if (rawList == null) return;

        ItemStack[] contents = new ItemStack[rawList.size()];
        for (int i = 0; i < rawList.size(); i++) {
            Object item = rawList.get(i);
            if (item instanceof ItemStack) {
                contents[i] = (ItemStack) item;
            } else {
                contents[i] = null;
            }
        }

        p.getInventory().setContents(contents);
        invCfg.set("inventory." + p.getName().toLowerCase(), null);
        try {
            invCfg.save(invFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!isLogged.getOrDefault(e.getPlayer().getName().toLowerCase(), false)) e.setCancelled(true);

    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (!isLogged.getOrDefault(e.getPlayer().getName().toLowerCase(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!isLogged.getOrDefault(e.getPlayer().getName().toLowerCase(), false)) {
            if (!e.getMessage().toLowerCase().startsWith("/login")) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "Vous devez vous connecter avec /login <token>");
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            if (!isLogged.getOrDefault(p.getName().toLowerCase(), false)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            if (!isLogged.getOrDefault(p.getName().toLowerCase(), false)) {
                e.setCancelled(true);
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (!cmd.getName().equalsIgnoreCase("login")) return false;
        if (args.length != 1) {
            p.sendMessage(ChatColor.RED + "Usage : /login <token>");
            return true;
        }

        String name = p.getName().toLowerCase();
        String expectedToken = data.getString("players." + name + ".token");
        if (args[0].equals(expectedToken)) {
            isLogged.put(name, true);
            data.set("players." + name + ".ip", p.getAddress().getAddress().getHostAddress());
            saveData();
            p.sendMessage(ChatColor.GREEN + "Connexion réussie !");
            restoreInventory(p);
        } else {
            p.sendMessage(ChatColor.RED + "Token invalide.");
        }
        return true;

    }



    class LoginChecker extends BukkitRunnable {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName().toLowerCase();
                if (!isLogged.getOrDefault(name, true)) {
                    long time = loginTimeout.getOrDefault(name, now);
                    if ((now - time) > 15000) {
                        p.kickPlayer("Connexion expirée. Reconnectez-vous.");
                    }
                }
            }
        }
    }

    public FileConfiguration getDataFile() {
        return data;
    }


    public static Main getInstance() {

        return instance;

    }

}
