package me.usainsrht.sit;

import me.usainsrht.sit.command.CommandHandler;
import me.usainsrht.sit.command.SitCommand;
import me.usainsrht.sit.listeners.DismountListener;
import me.usainsrht.sit.listeners.InteractListener;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.morepaperlib.MorePaperLib;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class Sit extends JavaPlugin {

    private static Sit instance;
    private MorePaperLib morePaperLib;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.morePaperLib = new MorePaperLib(this);

        getServer().getPluginManager().registerEvents(new InteractListener(), this);
        getServer().getPluginManager().registerEvents(new DismountListener(), this);

        CommandHandler.register(new SitCommand("sit",
                "command to reload plugin's config",
                "/sit reload",
                Collections.emptyList()));

        int pluginId = 30679;
        Metrics metrics = new Metrics(this, pluginId);

        // Number of sittable layouts configured on this server
        metrics.addCustomChart(new SimplePie("sitable_layout_count", () -> {
            FileConfiguration cfg = getConfig();
            Set<String> layouts = cfg.getConfigurationSection("sitables").getKeys(false);
            return String.valueOf(layouts.size());
        }));

        // Entity type(s) used across all sittable layouts
        metrics.addCustomChart(new AdvancedPie("sitable_entity_types", () -> {
            FileConfiguration cfg = getConfig();
            Set<String> layouts = cfg.getConfigurationSection("sitables").getKeys(false);
            Map<String, Integer> map = new HashMap<>();
            for (String layout : layouts) {
                String entityType = cfg.getString("sitables." + layout + ".entity.type");
                if (entityType != null && !entityType.isEmpty()) {
                    map.merge(entityType, 1, Integer::sum);
                }
            }
            return map;
        }));

        // Check type(s) used across all sittable layouts (BLOCKDATA or BLOCKS)
        metrics.addCustomChart(new AdvancedPie("sitable_check_types", () -> {
            FileConfiguration cfg = getConfig();
            Set<String> layouts = cfg.getConfigurationSection("sitables").getKeys(false);
            Map<String, Integer> map = new HashMap<>();
            for (String layout : layouts) {
                String checkType = cfg.getString("sitables." + layout + ".check");
                if (checkType != null && !checkType.isEmpty()) {
                    map.merge(checkType, 1, Integer::sum);
                }
            }
            return map;
        }));
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    public static Sit getInstance() {
        return instance;
    }

    public MorePaperLib getMorePaperLib() {
        return morePaperLib;
    }

}
