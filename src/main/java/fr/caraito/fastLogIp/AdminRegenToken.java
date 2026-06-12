package fr.caraito.fastLogIp;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
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

        if (sender instanceof Player playerSender) {
            TextComponent message = new TextComponent("Le token du joueur " + args[0] + " a été régénéré : ");
            message.setColor(net.md_5.bungee.api.ChatColor.YELLOW);

            TextComponent tokenComponent = new TextComponent(newToken);
            tokenComponent.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
            tokenComponent.setUnderlined(true);
            tokenComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, newToken));
            tokenComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§eCliquez pour copier")));

            message.addExtra(tokenComponent);
            playerSender.spigot().sendMessage(message);
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Le token du joueur " + args[0] + " a été régénéré : §e" + newToken);
        }

        Player targetPlayer = Bukkit.getPlayerExact(args[0]);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            TextComponent message = new TextComponent("Votre token a été régénéré par un administrateur : ");
            message.setColor(net.md_5.bungee.api.ChatColor.YELLOW);

            TextComponent tokenComponent = new TextComponent(newToken);
            tokenComponent.setColor(net.md_5.bungee.api.ChatColor.GRAY);
            tokenComponent.setUnderlined(true);
            tokenComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, newToken));
            tokenComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§eCliquez pour copier")));

            TextComponent suffix = new TextComponent(" (Cliquer pour copier)");
            suffix.setColor(net.md_5.bungee.api.ChatColor.GRAY);
            suffix.setItalic(true);

            message.addExtra(tokenComponent);
            message.addExtra(suffix);

            targetPlayer.spigot().sendMessage(message);
        }

        return true;
    }
}
