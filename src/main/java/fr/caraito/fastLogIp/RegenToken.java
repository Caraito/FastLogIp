package fr.caraito.fastLogIp;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
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

            TextComponent message = new TextComponent("Voici votre nouveau token : ");
            message.setColor(net.md_5.bungee.api.ChatColor.YELLOW);

            TextComponent tokenComponent = new TextComponent(newToken);
            tokenComponent.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
            tokenComponent.setUnderlined(true);
            tokenComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, newToken));
            tokenComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§eCliquez pour copier")));

            TextComponent suffix = new TextComponent(" (Cliquer pour copier)");
            suffix.setColor(net.md_5.bungee.api.ChatColor.GRAY);
            suffix.setItalic(true);

            message.addExtra(tokenComponent);
            message.addExtra(suffix);

            player.spigot().sendMessage(message);

            return true;
        }

        return false;
    }
}
