package com.purpurmc.sit;

import org.bukkit.plugin.java.JavaPlugin;

public final class Sit extends JavaPlugin {

    public static Sit instance;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(new Events(), this);
        saveDefaultConfig();
        SitCommand.registerCommand();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Sit getInstance() {
        return instance;
    }

}
