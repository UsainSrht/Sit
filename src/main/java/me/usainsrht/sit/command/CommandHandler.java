package me.usainsrht.sit.command;

import me.usainsrht.sit.Compat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

public class CommandHandler {

    public static void register(Command... cmds) {
        CommandMap commandMap = Compat.commandMap();
        if (commandMap == null) {
            throw new IllegalStateException("Could not find the server command map");
        }
        for (Command cmd : cmds) {
            commandMap.register(cmd.getName(), cmd);
        }
    }
}
