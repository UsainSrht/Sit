package me.usainsrht.sit.mod;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Platform independent sitting logic for the Fabric / Forge / NeoForge builds. This is a faithful port of the
 * Bukkit {@code InteractListener}: same layouts, same checks, same yaw and offset maths, same config file.
 */
public final class SitMod {

    public static final Logger LOG = Logger.getLogger("Sit");
    private static final String SEAT_TAG = "sit_seat";

    private static final boolean DEBUG = Boolean.getBoolean("sit.debug");

    static void debug(String message) {
        if (DEBUG) LOG.info("[debug] " + message);
    }

    private static volatile SitConfig config;
    private static volatile Mc mc;
    private static volatile boolean ready;
    private static File configDir = new File("config", "Sit");

    /** Bukkit BlockData classes that map onto a vanilla block class. */
    private static final Map<String, String> BLOCK_CLASSES = new HashMap<>();
    /** Bukkit BlockData interfaces that map onto the presence of a block state property. */
    private static final Map<String, String> PROPERTY_INTERFACES = new HashMap<>();

    static {
        String t = "org.bukkit.block.data.type.";
        BLOCK_CLASSES.put(t + "Stairs", "StairBlock");
        BLOCK_CLASSES.put(t + "Slab", "SlabBlock");
        BLOCK_CLASSES.put(t + "Fence", "FenceBlock");
        BLOCK_CLASSES.put(t + "Wall", "WallBlock");
        BLOCK_CLASSES.put(t + "Gate", "FenceGateBlock");
        BLOCK_CLASSES.put(t + "Door", "DoorBlock");
        BLOCK_CLASSES.put(t + "TrapDoor", "TrapDoorBlock");
        BLOCK_CLASSES.put(t + "Bed", "BedBlock");
        BLOCK_CLASSES.put(t + "Chest", "ChestBlock");
        BLOCK_CLASSES.put(t + "Ladder", "LadderBlock");
        String d = "org.bukkit.block.data.";
        PROPERTY_INTERFACES.put(d + "Directional", "facing");
        PROPERTY_INTERFACES.put(d + "Bisected", "half");
        PROPERTY_INTERFACES.put(d + "Rotatable", "rotation");
        PROPERTY_INTERFACES.put(d + "Waterlogged", "waterlogged");
        PROPERTY_INTERFACES.put(d + "Openable", "open");
        PROPERTY_INTERFACES.put(d + "Powerable", "powered");
        PROPERTY_INTERFACES.put(d + "Lightable", "lit");
        PROPERTY_INTERFACES.put(d + "Orientable", "axis");
        PROPERTY_INTERFACES.put(d + "AnaloguePowerable", "power");
        PROPERTY_INTERFACES.put(d + "Snowable", "snowy");
        PROPERTY_INTERFACES.put(d + "Levelled", "level");
        PROPERTY_INTERFACES.put(d + "Ageable", "age");
        PROPERTY_INTERFACES.put(d + "Attachable", "attached");
        PROPERTY_INTERFACES.put(d + "Hangable", "hanging");
    }

    private SitMod() {}

    /** Called once by every loader entrypoint. */
    public static synchronized void init(String loader) {
        if (ready) return;
        config = SitConfig.load(configDir);
        mc = Mc.bind();
        if (mc.problems().isEmpty()) {
            ready = true;
            LOG.info("Sit enabled on " + loader + " with " + config.layouts().size() + " sitable layout(s)");
        } else {
            LOG.severe("Sit could not bind to this Minecraft version, it stays disabled. Missing: " + mc.problems());
        }
    }

    static Mc mc() {
        return mc;
    }

    public static boolean isReady() {
        return ready;
    }

    public static void reload() {
        config = SitConfig.load(configDir);
    }

    // ------------------------------------------------------------------ interaction

    /**
     * Handles a right click on a block.
     *
     * @return true when the click was turned into sitting and the interaction must be consumed
     */
    public static boolean onUse(Object player, Object level, Object pos, Object hand) {
        if (!ready) return false;
        try {
            return handle(player, level, pos, hand);
        } catch (RuntimeException e) {
            LOG.warning("Sit failed to handle an interaction: " + e);
            return false;
        }
    }

    private static boolean handle(Object p, Object level, Object pos, Object hand) {
        Mc mc = SitMod.mc;
        debug("use event: clientSide=" + mc.isClientSide(level) + " hand=" + hand);
        if (mc.isClientSide(level) || !mc.isMainHand(hand)) return false;

        Mc.State state = mc.stateAt(level, pos);
        debug("block=" + state.id + " props=" + state.props + " class=" + state.block.getClass().getName());

        // Same implicit rules as the plugin: only the sittable half of stairs and slabs.
        if (isInstance("StairBlock", state) && !"bottom".equals(state.props.get("half"))) return false;
        if (isInstance("SlabBlock", state) && !"bottom".equals(state.props.get("type"))) return false;

        if (mc.isSneaking(p)) { debug("ignored: sneaking"); return false; }
        if (!mc.mainHandEmpty(p)) { debug("ignored: item in hand"); return false; }
        if (mc.isPassenger(p)) { debug("ignored: already riding"); return false; }

        SitConfig.Layout selected = null;
        for (SitConfig.Layout layout : config.layouts()) {
            if (layout.permissionRequired) {
                String permission = layout.permissionName;
                if (permission != null && !permission.isEmpty()) {
                    permission = String.format(permission, layout.name);
                    if (!Permissions.has(p, permission, mc)) break;
                }
            }
            if ("BLOCKDATA".equals(layout.check)) {
                for (String cls : layout.list) {
                    if (matchesBlockData(cls, state)) {
                        selected = layout;
                        break;
                    }
                }
            } else if ("BLOCKS".equals(layout.check)) {
                String path = state.id.substring(state.id.indexOf(':') + 1);
                for (String material : layout.list) {
                    if (path.equalsIgnoreCase(material)) {
                        selected = layout;
                        break;
                    }
                }
            }
            if (selected != null) break;
        }
        if (selected == null || selected.entityType == null) { debug("ignored: no layout matches"); return false; }

        int[] b = mc.posXYZ(pos);
        double x = b[0] + selected.x, y = b[1] + selected.y, z = b[2] + selected.z;

        float yaw = 0;
        String facing = state.props.get("facing");
        if (facing != null) {
            switch (facing) {
                case "south": yaw = 180; break;
                case "west": yaw = 270; break;
                case "east": yaw = 90; break;
                case "north": yaw = 0; break;
                default: break;
            }
        } else {
            yaw = mc.yaw(p) + 180;
        }
        if (isInstance("StairBlock", state)) {
            String shape = state.props.get("shape");
            if ("inner_right".equals(shape) || "outer_right".equals(shape)) yaw += 45;
            else if ("inner_left".equals(shape) || "outer_left".equals(shape)) yaw -= 45;
        }

        Object seat = mc.spawnSeat(level, p, selected.entityType, x, y, z, yaw, SEAT_TAG);
        if (seat == null) { debug("seat spawn failed"); return false; }
        debug("seated on " + selected.name);
        Seats.track(seat, 0);
        return true;
    }

    private static boolean isInstance(String blockClassKey, Mc.State state) {
        Class<?> c = Names.cls(blockClassKey);
        return c != null && c.isInstance(state.block);
    }

    private static final Set<String> WARNED = new HashSet<>();

    private static boolean matchesBlockData(String bukkitClass, Mc.State state) {
        if (bukkitClass.equals("org.bukkit.block.data.BlockData")) return true;
        String blockKey = BLOCK_CLASSES.get(bukkitClass);
        if (blockKey != null) return isInstance(blockKey, state);
        String property = PROPERTY_INTERFACES.get(bukkitClass);
        if (property != null) return state.props.containsKey(property);
        if (WARNED.add(bukkitClass)) LOG.warning("BlockData class '" + bukkitClass + "' is not supported by the mod build, it is ignored");
        return false;
    }

    // ------------------------------------------------------------------ seats

    /** Tracks seat entities and removes them when nobody sits on them any more (Bukkit's dismount listener). */
    static final class Seats {
        private static final class Seat {
            final Object entity;
            int grace;
            int age;

            Seat(Object entity, int grace) {
                this.entity = entity;
                this.grace = grace;
            }
        }

        private static final List<Seat> SEATS = new ArrayList<>();

        static void track(Object seat, int grace) {
            synchronized (SEATS) {
                for (Seat s : SEATS) {
                    if (s.entity == seat) {
                        // registered twice (once by the load event, once by the spawn): the shorter grace wins
                        s.grace = Math.min(s.grace, grace);
                        return;
                    }
                }
                SEATS.add(new Seat(seat, grace));
            }
        }

        static void tick() {
            Mc mc = SitMod.mc;
            synchronized (SEATS) {
                for (Iterator<Seat> it = SEATS.iterator(); it.hasNext(); ) {
                    Seat seat = it.next();
                    try {
                        if (mc.isRemoved(seat.entity)) {
                            it.remove();
                        } else if (!mc.isVehicle(seat.entity) && ++seat.age > seat.grace) {
                            debug("seat abandoned, discarding");
                            mc.discard(seat.entity);
                            it.remove();
                        }
                    } catch (RuntimeException e) {
                        debug("seat tick failed, no longer tracked: " + e + " / " + e.getCause());
                        it.remove();
                    }
                }
            }
        }
    }

    /**
     * A seat entity was loaded from disk (a player logged out while sitting and vanilla restored their mount).
     * It is tracked again, with a grace period so its rider can re-attach before it is considered abandoned.
     */
    public static void onEntityLoaded(Object entity) {
        if (!ready || entity == null) return;
        try {
            if (mc.hasTag(entity, SEAT_TAG)) Seats.track(entity, 100);
        } catch (RuntimeException e) {
            debug("entity load check failed: " + e);
        }
    }

    /** Called every server tick by every loader entrypoint. */
    public static void tick() {
        if (ready) Seats.tick();
    }

    // ------------------------------------------------------------------ permissions

    static final class Permissions {
        static boolean has(Object player, String node, Mc mc) {
            return mc.isOp(player) || PermissionHooks.check(player, node);
        }
    }

    /** Optional integration with permission mods (currently the Fabric Permissions API, which LuckPerms provides). */
    public static final class PermissionHooks {
        private static java.lang.reflect.Method fabricCheck;
        private static boolean looked;

        static boolean check(Object player, String node) {
            if (!looked) {
                looked = true;
                Class<?> c = Names.load("me.lucko.fabric.api.permissions.v0.Permissions");
                if (c != null) {
                    for (java.lang.reflect.Method m : c.getMethods()) {
                        if (m.getName().equals("check") && m.getParameterTypes().length == 3 && m.getParameterTypes()[1] == String.class
                                && m.getParameterTypes()[2] == boolean.class && m.getParameterTypes()[0].isAssignableFrom(Names.cls("Entity"))) {
                            fabricCheck = m;
                        }
                    }
                }
            }
            if (fabricCheck == null) return false;
            try {
                return (Boolean) fabricCheck.invoke(null, player, node, false);
            } catch (ReflectiveOperationException | RuntimeException e) {
                return false;
            }
        }
    }

}
