package me.usainsrht.sit;

import me.usainsrht.sit.command.CommandHandler;
import me.usainsrht.sit.command.SitCommand;
import me.usainsrht.sit.listeners.DismountListener;
import me.usainsrht.sit.listeners.InteractListener;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;

public final class Sit extends JavaPlugin {

    private static Sit instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new InteractListener(), this);
        getServer().getPluginManager().registerEvents(new DismountListener(), this);

        CommandHandler.register(new SitCommand("sit",
                "command to reload plugin's config",
                "/sit reload",
                Collections.emptyList()));
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    public static Sit getInstance() {
        return instance;
    }

}
