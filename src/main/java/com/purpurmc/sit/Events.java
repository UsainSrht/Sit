package com.purpurmc.sit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Steerable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.spigotmc.event.entity.EntityDismountEvent;

public class Events implements Listener {

    @EventHandler
    public void onSit(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block b = e.getClickedBlock();
            BlockData bd = b.getBlockData();
            if (bd instanceof Stairs) {
                if (((Bisected) bd).getHalf() == Bisected.Half.BOTTOM) {
                    Player p = e.getPlayer();
                    if (!p.isSneaking()) {
                        if (p.getInventory().getItemInMainHand().getType() == Material.AIR) {
                            if (!p.isInsideVehicle()) {
                                e.setCancelled(true);

                                Location loc = b.getLocation();
                                loc.setX(loc.getX() + 0.5);
                                loc.setY(loc.getY() - 0.4);
                                loc.setZ(loc.getZ() + 0.5);

                                BlockFace facing = ((Directional) bd).getFacing();
                                switch (facing) {
                                    case SOUTH: loc.setYaw(180);
                                                break;
                                    case WEST: loc.setYaw(270);
                                                break;
                                    case EAST: loc.setYaw(90);
                                                break;
                                    case NORTH: loc.setYaw(0);
                                                break;
                                }

                                Stairs.Shape shape = ((Stairs) bd).getShape();
                                if (shape == Stairs.Shape.INNER_RIGHT || shape == Stairs.Shape.OUTER_RIGHT) {
                                    loc.setYaw(loc.getYaw()+45);
                                }
                                else if (shape == Stairs.Shape.INNER_LEFT || shape == Stairs.Shape.OUTER_LEFT) {
                                    loc.setYaw(loc.getYaw()-45);
                                }

                                Entity stair = p.getWorld().spawnEntity(loc, EntityType.PIG);
                                ((Steerable) stair).setSaddle(true);
                                ((Steerable) stair).addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100000, 1, false, false, false));
                                stair.setInvulnerable(true);
                                stair.setSilent(true);
                                ((Steerable) stair).getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
                                stair.setMetadata("stair", new FixedMetadataValue(Sit.instance, true));

                                stair.addPassenger(p);

                                ((Steerable) stair).setAI(false);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent e) {
        if (e.getDismounted().hasMetadata("stair")) {
            Bukkit.getScheduler().runTaskLater(Sit.instance, () -> e.getDismounted().remove(), 1L);
        }
    }
}
