package me.usainsrht.sit.listeners;

import me.usainsrht.sit.Sit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attributable;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Set;

public class InteractListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSit(PlayerInteractEvent e) {
        if (e.getHand() == null || !e.getHand().equals(EquipmentSlot.HAND)) return;

        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        Block block = e.getClickedBlock();
        BlockData blockData = block.getBlockData();

        if (blockData instanceof Stairs) {
            Bisected bisected = (Bisected) blockData;
            if (!bisected.getHalf().equals(Bisected.Half.BOTTOM)) return;
        }
        else if (blockData instanceof Slab) {
            if (!((Slab) blockData).getType().equals(Slab.Type.BOTTOM)) return;
        }

        Player p = e.getPlayer();

        if (p.isSneaking()) return;

        if (!p.getInventory().getItemInMainHand().getType().equals(Material.AIR)) return;

        if (p.isInsideVehicle()) return;

        Sit instance = Sit.getInstance();
        FileConfiguration config = instance.getConfig();
        String selectedLayout = null;

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
                        if (claz.isInstance(blockData)) {
                            selectedLayout = node;
                            break;
                        }
                    }
                    break;
                case "BLOCKS":
                    for (String material : config.getStringList("sitables." + node + ".list")) {
                        if (block.getType().toString().equalsIgnoreCase(material)) {
                            selectedLayout = node;
                            break;
                        }
                    }
                    break;
            }
            if (selectedLayout != null) break;
        }
        if (selectedLayout == null) return;

        e.setCancelled(true);

        Location loc = block.getLocation();

        double adderX = config.getDouble("sitables." + selectedLayout + ".offsets.x");
        double adderY = config.getDouble("sitables." + selectedLayout + ".offsets.y");
        double adderZ = config.getDouble("sitables." + selectedLayout + ".offsets.z");

        loc.setX(loc.getX() + adderX);
        loc.setY(loc.getY() + adderY);
        loc.setZ(loc.getZ() + adderZ);

        if (blockData instanceof Directional) {
            BlockFace facing = ((Directional) blockData).getFacing();
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

        if (blockData instanceof Stairs) {
            Stairs.Shape shape = ((Stairs) blockData).getShape();
            if (shape == Stairs.Shape.INNER_RIGHT || shape == Stairs.Shape.OUTER_RIGHT) {
                loc.setYaw(loc.getYaw()+45);
            }
            else if (shape == Stairs.Shape.INNER_LEFT || shape == Stairs.Shape.OUTER_LEFT) {
                loc.setYaw(loc.getYaw()-45);
            }
        }

        String entityType = config.getString("sitables." + selectedLayout + ".entity.type");
        // create final value to use in lambda
        final String layout = selectedLayout;
        Entity entity = p.getWorld().spawn(loc, EntityType.valueOf(entityType).getEntityClass(), (stair -> {
            if (stair instanceof Attributable) {
                Attributable attributable = (Attributable) stair;
                // set movement speed to 0 to entity to not move when steering item(carrot on a stick) held
                attributable.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0);

                if (config.getBoolean("sitables." + layout + ".entity.saddle")) {
                    steerable.setSaddle(true);
                }
            }

            stair.setInvulnerable(true);
            stair.setSilent(true);
            stair.setMetadata("stair", new FixedMetadataValue(instance, true));

            if (stair instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) stair;
                livingEntity.setInvisible(true);
                livingEntity.setAI(false);
            }
        }));

        entity.addPassenger(p);
    }
}
