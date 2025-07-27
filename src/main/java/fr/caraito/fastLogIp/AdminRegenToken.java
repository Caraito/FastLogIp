package fr.caraito.fastLogIp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class AdminRegenToken implements CommandExecutor {

    private final FileConfiguration data;
    private final Main main;

    public AdminRegenToken(Main main) {
        this.main = main;
        this.data = main.getDataFile();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!command.getName().equalsIgnoreCase("adminregentoken")) return false;

        if (!sender.hasPermission("fastlogip.admin")) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage : /adminregentoken <pseudo>");
            return true;
        }

        String targetName = args[0].toLowerCase();

        if (!data.contains("players." + targetName)) {
            sender.sendMessage(ChatColor.RED + "Le joueur " + args[0] + " n'a pas de token enregistré.");
            return true;
        }

        String newToken = main.generateToken(8);
        data.set("players." + targetName + ".token", newToken);
        main.saveData();

        sender.sendMessage(ChatColor.YELLOW + "Le token du joueur " + args[0] + " a été régénéré : §e" + newToken);

        Player targetPlayer = Bukkit.getPlayerExact(args[0]);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(ChatColor.YELLOW + "Votre token a été régénéré par un administrateur : §7" + newToken);
        }

        return true;
    }
}
