package fr.caraito.fastLogIp;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class RegenToken implements CommandExecutor {

    private final FileConfiguration data;
    private final Main main;

    public RegenToken(Main main) {
        this.main = main;
        this.data = main.getDataFile();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) return true;

        if (command.getName().equalsIgnoreCase("regentoken")) {

            boolean isLog = Main.isLogged.getOrDefault(player.getName().toLowerCase(), false);

            if (!isLog) {
                player.sendMessage(ChatColor.RED + "Vous devez être connecté pour régénérer votre token.");
                return true;
            }

            String newToken = main.generateToken(8);
            data.set("players." + player.getName().toLowerCase() + ".token", newToken);
            main.saveData();
            player.sendMessage(ChatColor.YELLOW + "Voici votre nouveau token : §e" + newToken);

            return true;
        }

        return false;
    }
}
