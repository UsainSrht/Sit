package me.usainsrht.sit.listeners;

import me.usainsrht.sit.Sit;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.entity.EntityDismountEvent;


public class DismountListener implements Listener {

    @EventHandler
    public void onDismount(EntityDismountEvent e) {
        if (e.getDismounted().hasMetadata("stair")) {
            Bukkit.getScheduler().runTaskLater(Sit.getInstance(), () -> e.getDismounted().remove(), 1L);
        }
    }
}
