package com.purpurmc.sit.events;

import com.purpurmc.sit.Sit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

public class onSit implements Listener {

    @EventHandler
    public void onSit(PlayerInteractEvent e) {
        if (e.getHand() == null || !e.getHand().equals(EquipmentSlot.HAND)) return;

        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        Block b = e.getClickedBlock();
        BlockData bd = b.getBlockData();
        String offsetmode = "";

        Sit instance = Sit.getInstance();
        FileConfiguration config = instance.getConfig();

        Player p = e.getPlayer();

        if (p.isSneaking()) return;

        if (!p.getInventory().getItemInMainHand().getType().equals(Material.AIR)) return;

        if (p.isInsideVehicle()) return;

        Set<String> nodes = config.getConfigurationSection("sitables").getKeys(false);
        for (String node : nodes) {

            if (config.getBoolean("sitables." + node + ".permission.require")) {
                String permission = config.getString("sitables." + node + ".permission.name");
                if (permission != null && !permission.equals("")) {
                    permission = String.format(permission, node);
                    if (!p.hasPermission(permission)) break;
                }
            }

            String mode = config.getString("sitables." + node + ".check");
            switch (mode) {
                case "BLOCKDATA":
                    Class claz;
                    for (String clasz : config.getStringList("sitables." + node + ".list")) {
                        try {
                            claz = Class.forName(clasz);
                        }
                        catch (ClassNotFoundException ex) {
                            throw new RuntimeException("class " + clasz + " doesn't exists.");
                        }
                        if (claz.isInstance(bd)) {
                            offsetmode = node;
                            break;
                        }
                    }
                    break;
                case "BLOCKS":
                    for (String material : config.getStringList("sitables." + node + ".list")) {
                        if (b.getType().toString().equalsIgnoreCase(material)) {
                            offsetmode = node;
                            break;
                        }
                    }
                    break;
            }
            if (offsetmode.length() > 0) break;
        }
        if (!(offsetmode.length() > 0)) return;

        if (bd instanceof Stairs) {
            Bisected bisected = (Bisected) bd;
            if (!bisected.getHalf().equals(Bisected.Half.BOTTOM)) return;
        }
        else if (bd instanceof Slab) {
            if (!((Slab) bd).getType().equals(Slab.Type.BOTTOM)) return;
        }

        e.setCancelled(true);

        Location loc = b.getLocation();

        double adderx = config.getDouble("sitables." + offsetmode + ".offsets.x");
        double addery = config.getDouble("sitables." + offsetmode + ".offsets.y");
        double adderz = config.getDouble("sitables." + offsetmode + ".offsets.z");

        loc.setX(loc.getX() + adderx);
        loc.setY(loc.getY() + addery);
        loc.setZ(loc.getZ() + adderz);

        if (bd instanceof Directional) {
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
        }
        else {
            loc.setYaw(p.getLocation().getYaw());
        }

        if (bd instanceof Stairs) {
            Stairs.Shape shape = ((Stairs) bd).getShape();
            if (shape == Stairs.Shape.INNER_RIGHT || shape == Stairs.Shape.OUTER_RIGHT) {
                loc.setYaw(loc.getYaw()+45);
            }
            else if (shape == Stairs.Shape.INNER_LEFT || shape == Stairs.Shape.OUTER_LEFT) {
                loc.setYaw(loc.getYaw()-45);
            }
        }

        Location spawnLoc = loc.clone();
        spawnLoc.setY(255);
        String entityType = config.getString("sitables." + offsetmode + ".entity.type");
        Entity stair = p.getWorld().spawnEntity(spawnLoc, EntityType.valueOf(entityType));

        if (stair instanceof Steerable) {
            Steerable steerable = (Steerable) stair;

            steerable.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100000, 1, false, false, false));
            steerable.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0);

            if (config.getBoolean("sitables." + offsetmode + ".entity.saddle")) {
                steerable.setSaddle(true);
            }
        }

        stair.setInvulnerable(true);
        stair.setSilent(true);
        stair.setMetadata("stair", new FixedMetadataValue(instance, true));
        stair.teleport(loc);

        Long ticksBeforeMount = (Long) config.getLong("sitables." + offsetmode + ".ticks_before_mount");
        Bukkit.getScheduler().runTaskLater(Sit.getInstance(), () -> stair.addPassenger(p), ticksBeforeMount);
        
        if (stair instanceof ArmorStand) {
            ((ArmorStand) stair).setVisible(false);
        }
        else if (stair instanceof LivingEntity){
            ((LivingEntity)stair).setAI(false);
        }
    }
 }
