package me.usainsrht.sit;

import me.usainsrht.sit.command.CommandHandler;
import me.usainsrht.sit.command.SitCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;

public final class Sit extends JavaPlugin {

    private static Sit instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        //todo choose either making the plugin based on 1.13(blockdatas) or add support for 1.8 too

        CommandHandler.register(new SitCommand("sit",
                "command to reload plugin's config",
                "/sit reload",
                Collections.emptyList()));
    }

    @Override
    public void onDisable() {
    }

    public static Sit getInstance() {
        return instance;
    }

}
