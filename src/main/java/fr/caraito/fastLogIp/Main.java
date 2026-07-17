package fr.caraito.fastLogIp;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
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
import java.util.logging.Level;

public class Main extends JavaPlugin implements Listener {

    private FileConfiguration data;
    private File dataFile;
    public static final Map<String, Boolean> isLogged = new HashMap<>();
    public static final Map<String, Long> loginTimeout = new HashMap<>();
    public static final Map<String, Boolean> isInventoryEmpty = new HashMap<>();
    private static Main instance;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_LOCKOUT_MS = 60_000L;
    private final Map<String, Integer> failedLoginAttempts = new HashMap<>();
    private final Map<String, Long> loginLockoutUntil = new HashMap<>();

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
                    if (!dataFile.createNewFile()) {
                        getLogger().severe("Impossible de créer data.yml (raison inconnue).");
                    }
                } catch (IOException e) {
                    getLogger().log(Level.SEVERE, "Impossible de créer data.yml, le plugin va se désactiver.", e);
                    getServer().getPluginManager().disablePlugin(this);
                    return;
                }
            }

        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public boolean saveData() {
        try {
            data.save(dataFile);
            return true;
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Erreur lors de la sauvegarde de data.yml", e);
            return false;
        }
    }

    public String generateToken(int length) {
        return TokenUtil.generateToken(length);
    }

    /**
     * Stocke le token sous forme de hash salé (jamais en clair) dans data.yml.
     */
    public void storeToken(String playerNameLower, String rawToken) {
        String salt = TokenUtil.generateSalt();
        String hash = TokenUtil.hash(rawToken, salt);
        data.set("players." + playerNameLower + ".tokenSalt", salt);
        data.set("players." + playerNameLower + ".tokenHash", hash);
        data.set("players." + playerNameLower + ".token", null);
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
            storeToken(p.getName().toLowerCase(), token);
            if (!saveData()) {
                p.sendMessage(ChatColor.RED + "Erreur interne lors de l'enregistrement de votre token. Contactez un administrateur.");
            }
            isLogged.put(p.getName().toLowerCase(), true);

            new BukkitRunnable() {
                @Override
                public void run() {
                    p.sendMessage(ChatColor.GREEN + "Bienvenue, vous êtes automatiquement connecté.");

                    TextComponent message = new TextComponent("Voici votre token : ");
                    message.setColor(net.md_5.bungee.api.ChatColor.YELLOW);

                    TextComponent tokenComponent = new TextComponent(token);
                    tokenComponent.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                    tokenComponent.setUnderlined(true);
                    tokenComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, token));
                    tokenComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§eCliquez pour copier")));

                    TextComponent suffix = new TextComponent(" (Cliquer pour copier)");
                    suffix.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                    suffix.setItalic(true);

                    message.addExtra(tokenComponent);
                    message.addExtra(suffix);

                    p.spigot().sendMessage(message);

                    p.sendMessage(ChatColor.GRAY + "Gardez-le précieusement !");
                    p.sendMessage(ChatColor.GRAY + "Il permettra de vous reconnecter");
                    sendActionBar(p, "§cVoici votre token : §e" + token, 200);

                }
            }.runTaskLater(this, 20);


        } else {
            String savedIp = data.getString("players." + p.getName().toLowerCase() + ".ip");
            if (currentIp.equals(savedIp)) {
                isLogged.put(p.getName().toLowerCase(), true);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        p.sendMessage(ChatColor.GREEN + "Connexion automatique réussie.");
                    }
                }.runTaskLater(this, 20);
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
            getLogger().log(Level.SEVERE, "Erreur lors de la sauvegarde de l'inventaire de " + p.getName(), ex);
            p.sendMessage(ChatColor.RED + "Erreur lors de la sauvegarde de votre inventaire. Contactez un administrateur immédiatement.");
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
            getLogger().log(Level.SEVERE, "Erreur lors de la sauvegarde de l'inventaire de " + p.getName() + " après restauration", e);
            p.sendMessage(ChatColor.RED + "Erreur lors de la sauvegarde de votre inventaire. Contactez un administrateur.");
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

        Long lockedUntil = loginLockoutUntil.get(name);
        if (lockedUntil != null && System.currentTimeMillis() < lockedUntil) {
            long remaining = (lockedUntil - System.currentTimeMillis() + 999) / 1000;
            p.sendMessage(ChatColor.RED + "Trop de tentatives échouées. Réessayez dans " + remaining + "s.");
            return true;
        }

        boolean valid = checkToken(name, args[0]);

        if (valid) {
            failedLoginAttempts.remove(name);
            loginLockoutUntil.remove(name);
            isLogged.put(name, true);
            data.set("players." + name + ".ip", p.getAddress().getAddress().getHostAddress());
            if (!saveData()) {
                p.sendMessage(ChatColor.RED + "Erreur interne lors de l'enregistrement de votre connexion. Contactez un administrateur.");
            }
            p.sendMessage(ChatColor.GREEN + "Connexion réussie !");
            restoreInventory(p);
        } else {
            int attempts = failedLoginAttempts.merge(name, 1, Integer::sum);
            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                loginLockoutUntil.put(name, System.currentTimeMillis() + LOGIN_LOCKOUT_MS);
                failedLoginAttempts.remove(name);
                p.sendMessage(ChatColor.RED + "Trop de tentatives échouées. Vous devez attendre " + (LOGIN_LOCKOUT_MS / 1000) + "s avant de réessayer.");
            } else {
                p.sendMessage(ChatColor.RED + "Token invalide.");
            }
        }
        return true;

    }

    /**
     * Vérifie le token fourni contre le hash salé stocké. Si un ancien token en clair
     * (avant migration vers le hashage) est trouvé et correspond, il est migré vers un
     * hash salé automatiquement.
     */
    private boolean checkToken(String nameLower, String providedToken) {
        String storedHash = data.getString("players." + nameLower + ".tokenHash");
        String storedSalt = data.getString("players." + nameLower + ".tokenSalt");

        if (storedHash != null && storedSalt != null) {
            return TokenUtil.matches(providedToken, storedSalt, storedHash);
        }

        String legacyToken = data.getString("players." + nameLower + ".token");
        if (legacyToken != null && TokenUtil.constantTimeEquals(providedToken, legacyToken)) {
            storeToken(nameLower, providedToken);
            saveData();
            return true;
        }

        return false;
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

    public void sendActionBar(Player player, String message, int durationTicks) {
        new BukkitRunnable() {
            int timer = durationTicks;
            @Override
            public void run() {
                if (timer <= 0) {
                    cancel();
                    return;
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
                timer--;
            }
        }.runTaskTimer(this, 0, 1);
    }
}






