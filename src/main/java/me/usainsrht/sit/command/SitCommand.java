package me.usainsrht.sit.command;

import me.usainsrht.sit.Sit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class SitCommand extends Command {

    public SitCommand(String name, String description, String usageMessage, List<String> aliases) {
        super(name, description, usageMessage, aliases);
    }

    @Override
    public boolean execute(CommandSender sender, String command, String[] args) {
        if (sender.hasPermission("sit.command"))
        {
            if (args.length == 0) {
                sender.sendMessage("/sit reload");
            }
            else if (args[0].equalsIgnoreCase("reload")) {
                Sit.getInstance().reloadConfig();

                sender.sendMessage(ChatColor.GREEN+"config reloaded succesfully");
            }
            else {
                sender.sendMessage("/sit reload");
            }
        }
        return true;
    }

}
