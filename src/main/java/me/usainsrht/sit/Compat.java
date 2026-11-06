package me.usainsrht.sit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Runtime shims for Bukkit API that changed shape between 1.13 and the latest versions
 * (enum -> interface conversions, renamed constants, removed overloads).
 * Everything here is resolved reflectively so one jar links on every server version.
 */
public final class Compat {

    private Compat() {}

    private static final Attribute MOVEMENT_SPEED = findAttribute("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED");

    private static Attribute findAttribute(String... names) {
        for (String name : names) {
            try {
                Object value = Attribute.class.getField(name).get(null);
                if (value instanceof Attribute) return (Attribute) value;
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
        return null;
    }

    /** Sets base movement speed to 0 so steering items (carrot on a stick) can't move the seat. */
    public static void freezeMovement(Entity entity) {
        if (MOVEMENT_SPEED == null || !(entity instanceof Attributable)) return;
        try {
            AttributeInstance attribute = ((Attributable) entity).getAttribute(MOVEMENT_SPEED);
            if (attribute != null) attribute.setBaseValue(0);
        } catch (LinkageError ignored) {
        }
    }

    /** Equips a saddle on a pig, across the old setSaddle API and the 1.21.5+ equipment-slot saddle. */
    public static void saddle(Pig pig) {
        try {
            pig.setSaddle(true);
            return;
        } catch (LinkageError ignored) {
            // setSaddle(boolean) is gone on this version, fall through to the equipment slot
        }
        try {
            Object slot = Class.forName("org.bukkit.inventory.EquipmentSlot").getField("SADDLE").get(null);
            EntityEquipment equipment = pig.getEquipment();
            if (equipment == null) return;
            Method setItem = EntityEquipment.class.getMethod("setItem", slot.getClass(), ItemStack.class);
            setItem.invoke(equipment, slot, new ItemStack(Material.valueOf("SADDLE")));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    /** Makes a living entity invisible without depending on the PotionEffectType class shape. */
    public static void makeInvisible(LivingEntity entity) {
        try {
            Method setInvisible = LivingEntity.class.getMethod("setInvisible", boolean.class);
            setInvisible.invoke(entity, true);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        // Potion effect is kept as well: it is what the plugin always did, and it covers 1.13-1.16.
        try {
            PotionEffectType type = PotionEffectType.getByName("INVISIBILITY");
            if (type != null) {
                entity.addPotionEffect(new PotionEffect(type, 99999, 1, false, false));
            }
        } catch (LinkageError | RuntimeException ignored) {
        }
    }

    /** Finds the server CommandMap on every version (API method when present, field otherwise). */
    public static CommandMap commandMap() {
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getCommandMap");
            Object map = method.invoke(Bukkit.getServer());
            if (map instanceof CommandMap) return (CommandMap) map;
        } catch (ReflectiveOperationException ignored) {
        }
        Class<?> type = Bukkit.getServer().getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("commandMap");
                field.setAccessible(true);
                return (CommandMap) field.get(Bukkit.getServer());
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
            type = type.getSuperclass();
        }
        return null;
    }
}
