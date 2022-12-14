package com.purpurmc.sit;

import com.purpurmc.sit.events.onDismount;
import com.purpurmc.sit.events.onSit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public final class Sit extends JavaPlugin {

    public static Sit instance;

    @Override
    public void onEnable() {
        instance = this;

        getServer().getPluginManager().registerEvents(new onSit(), this);
        getServer().getPluginManager().registerEvents(new onDismount(), this);

        saveDefaultConfig();

        CommandHandler.register(new SitCommand("sit",
                "command to reload plugin's config",
                "/sit reload",
                new ArrayList<>()));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Sit getInstance() {
        return instance;
    }

}
