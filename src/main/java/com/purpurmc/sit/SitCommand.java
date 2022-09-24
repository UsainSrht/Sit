package com.purpurmc.sit;

import com.mojang.brigadier.CommandDispatcher;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

public class SitCommand {

    public static void registerCommand() {
        Sit.getInstance().getLogger().info("register method called");
        CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();

        dispatcher.register(
                literal("sit")
                        .then(
                                argument("reload", StringArgument.string())
                                        .executes(c -> {
                                            ((CommandSender)c.getSource()).sendMessage(Component.text("reloaded"));
                                            return 1;
                                        })
                        )
                        .executes(c -> {
                            ((CommandSender)c.getSource()).sendMessage(Component.text("called without arguments"));
                            return 1;
                        })
        );
        Sit.getInstance().getLogger().info("register method finished");
    }
}
