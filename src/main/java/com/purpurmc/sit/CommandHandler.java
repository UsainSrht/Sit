package com.purpurmc.sit;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;

public class CommandHandler {

    public static void register(Command... cmds) {
        CommandMap commandMap = null;
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        for (Command cmd : cmds) {
            commandMap.register(cmd.getName(), cmd);
        }
    }
}
