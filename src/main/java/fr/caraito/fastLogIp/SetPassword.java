package fr.caraito.fastLogIp;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetPassword implements CommandExecutor {

    private static final int MIN_LENGTH = 6;

    private final Main main;

    public SetPassword(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) return true;
        if (!command.getName().equalsIgnoreCase("setpassword")) return false;

        boolean isLog = Main.isLogged.getOrDefault(player.getName().toLowerCase(), false);
        if (!isLog) {
            player.sendMessage(ChatColor.RED + "Vous devez être connecté pour définir un mot de passe.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage : /setpassword <mot de passe>");
            return true;
        }

        String password = args[0];
        if (password.length() < MIN_LENGTH) {
            player.sendMessage(ChatColor.RED + "Le mot de passe doit contenir au moins " + MIN_LENGTH + " caractères.");
            return true;
        }

        main.storePassword(player.getName().toLowerCase(), password);
        if (!main.saveData()) {
            player.sendMessage(ChatColor.RED + "Erreur interne lors de l'enregistrement du mot de passe. Contactez un administrateur.");
            return true;
        }

        player.sendMessage(ChatColor.GREEN + "Votre mot de passe personnalisé a été enregistré.");
        player.sendMessage(ChatColor.GRAY + "Vous pouvez désormais vous connecter avec /login <mot de passe> ou avec votre token d'origine.");

        return true;
    }
}
