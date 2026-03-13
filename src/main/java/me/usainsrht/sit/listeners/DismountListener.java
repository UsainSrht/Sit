package me.usainsrht.sit.listeners;

import me.usainsrht.sit.Sit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.entity.EntityDismountEvent;


public class DismountListener implements Listener {

    @EventHandler
    public void onDismount(EntityDismountEvent e) {
        Entity entity = e.getDismounted();
        if (entity.hasMetadata("stair")) {
            Sit.getInstance().getMorePaperLib().scheduling().entitySpecificScheduler(entity).runDelayed(() -> {
                e.getDismounted().remove();
            }, () -> {}, 1L);

            //Bukkit.getScheduler().runTaskLater(Sit.getInstance(), () -> e.getDismounted().remove(), 1L);
        }
    }
}
