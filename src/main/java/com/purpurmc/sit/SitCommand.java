package com.purpurmc.sit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SitCommand extends Command {

    protected SitCommand(@NotNull String name, @NotNull String description, @NotNull String usageMessage, @NotNull List<String> aliases) {
        super(name, description, usageMessage, aliases);
    }

    @Override
    public boolean execute(CommandSender sender, String command, String[] args) {
        if (args[0].equalsIgnoreCase("reload")) {
            Sit.getInstance().reloadConfig();
            sender.sendMessage(Component.text("config reloaded succesfully", NamedTextColor.GREEN));
        }
        return false;
    }

}
