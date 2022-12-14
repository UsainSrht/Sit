package com.purpurmc.sit.events;

import com.purpurmc.sit.Sit;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.entity.EntityDismountEvent;


public class onDismount implements Listener {

    @EventHandler
    public void onDismount(EntityDismountEvent e) {
        if (e.getDismounted().hasMetadata("stair")) {
            Bukkit.getScheduler().runTaskLater(Sit.getInstance(), () -> e.getDismounted().remove(), 1L);
        }
    }
}
