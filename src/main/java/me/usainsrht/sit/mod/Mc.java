package me.usainsrht.sit.mod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every Minecraft call the mod makes, bound reflectively at startup through {@link Names}.
 * Nothing here references a Minecraft class at compile time, which is what lets one jar run on every version.
 */
public final class Mc {

    /** A parsed block state: registry id plus its properties as lowercase strings. */
    public static final class State {
        public final Object raw;
        public final Object block;
        public final String id;
        public final Map<String, String> props;

        State(Object raw, Object block, String id, Map<String, String> props) {
            this.raw = raw;
            this.block = block;
            this.id = id;
            this.props = props;
        }
    }

    private static final Pattern STATE = Pattern.compile("\\{([^{}:]+:[^{}]+)\\}(?:\\[(.*)\\])?$");

    private Class<?> entityC, entityTypeC, levelC, livingC, mobC;
    private Method moveTo, setNoGravity, setInvulnerable, setSilent, setInvisible, setNoAi, addTag;
    private Method startRiding1, startRiding2, startRiding3;
    private Method getTags;
    private Method isShiftKeyDown, isPassenger, isVehicle, discard, remove0, isRemoved, getYRotM;
    private Method getMainHandItem, itemIsEmpty, getBlockState, addFreshEntity, isClientSideM, getBlockOfState;
    private Method vec3iX, vec3iY, vec3iZ, entityByString, createSeat1, createSeat2;
    private Method createSourceStack, hasPermissionInt, permissionsM, permSetHas;
    private Field isClientSideF, yRotF, removedF, spawnReason, gamemasterPerm;
    private Object entityTypeRegistryObj;
    private final Map<String, Object> entityTypes = new HashMap<>();
    private final List<String> problems = new ArrayList<>();

    private Mc() {}

    public static Mc bind() {
        Mc mc = new Mc();
        mc.doBind();
        return mc;
    }

    public List<String> problems() {
        return Collections.unmodifiableList(problems);
    }

    private <T> T need(T value, String what) {
        if (value == null) problems.add(what);
        return value;
    }

    private void doBind() {
        entityC = need(Names.cls("Entity"), "class Entity");
        entityTypeC = need(Names.cls("EntityType"), "class EntityType");
        levelC = need(Names.cls("Level"), "class Level");
        livingC = need(Names.cls("LivingEntity"), "class LivingEntity");
        mobC = Names.cls("Mob");
        Class<?> vec3i = Names.cls("Vec3i");
        Class<?> itemStackC = Names.cls("ItemStack");
        Class<?> blockStateC = Names.cls("BlockState");

        moveTo = need(Names.method("Entity.moveTo", entityC, 5), "Entity.moveTo");
        setNoGravity = need(Names.method("Entity.setNoGravity", entityC, 1), "Entity.setNoGravity");
        setInvulnerable = need(Names.method("Entity.setInvulnerable", entityC, 1), "Entity.setInvulnerable");
        setSilent = need(Names.method("Entity.setSilent", entityC, 1), "Entity.setSilent");
        setInvisible = need(Names.method("Entity.setInvisible", entityC, 1), "Entity.setInvisible");
        addTag = Names.method("Entity.addTag", entityC, 1);
        getTags = Names.method("Entity.getTags", entityC, 0);
        setNoAi = Names.method("Mob.setNoAi", mobC, 1);
        startRiding1 = Names.method("Entity.startRiding1", entityC, 1);
        startRiding2 = Names.method("Entity.startRiding2", entityC, 2);
        startRiding3 = Names.method("Entity.startRiding3", entityC, 3);
        if (startRiding1 == null && startRiding2 == null && startRiding3 == null) problems.add("Entity.startRiding");
        isShiftKeyDown = need(Names.method("Entity.isShiftKeyDown", entityC, 0), "Entity.isShiftKeyDown");
        isPassenger = need(Names.method("Entity.isPassenger", entityC, 0), "Entity.isPassenger");
        isVehicle = need(Names.method("Entity.isVehicle", entityC, 0), "Entity.isVehicle");
        discard = Names.method("Entity.discard", entityC, 0);
        remove0 = Names.method("Entity.remove0", entityC, 0);
        if (discard == null && remove0 == null) problems.add("Entity.discard/remove");
        isRemoved = Names.method("Entity.isRemoved", entityC, 0);
        removedF = Names.field("Entity.removed", entityC);
        getYRotM = Names.method("Entity.getYRot", entityC, 0);
        yRotF = Names.field("Entity.yRot", entityC);
        if (getYRotM == null && yRotF == null) problems.add("Entity.getYRot");
        getMainHandItem = need(Names.method("LivingEntity.getMainHandItem", livingC, 0), "LivingEntity.getMainHandItem");
        itemIsEmpty = need(Names.method("ItemStack.isEmpty", itemStackC, 0), "ItemStack.isEmpty");
        getBlockState = need(Names.method("Level.getBlockState", levelC, 1), "Level.getBlockState");
        addFreshEntity = need(Names.method("Level.addFreshEntity", levelC, 1), "Level.addFreshEntity");
        isClientSideM = Names.method("Level.isClientSide", levelC, 0);
        isClientSideF = Names.field("Level.isClientSide", levelC);
        if (isClientSideM == null && isClientSideF == null) problems.add("Level.isClientSide");
        getBlockOfState = need(Names.method("BlockState.getBlock", blockStateC, 0), "BlockState.getBlock");
        vec3iX = need(Names.method("Vec3i.getX", vec3i, 0), "Vec3i.getX");
        vec3iY = need(Names.method("Vec3i.getY", vec3i, 0), "Vec3i.getY");
        vec3iZ = need(Names.method("Vec3i.getZ", vec3i, 0), "Vec3i.getZ");

        entityByString = Names.method("EntityType.byString", entityTypeC, 1);
        final Class<?> levelType = levelC;
        createSeat1 = Names.method("EntityType.create1", entityTypeC, 1, mm -> mm.getParameterTypes()[0] == levelType && !Modifier.isStatic(mm.getModifiers()));
        createSeat2 = Names.method("EntityType.create2", entityTypeC, 2, mm -> mm.getParameterTypes()[0] == levelType && !Modifier.isStatic(mm.getModifiers())
                && mm.getParameterTypes()[1].isEnum());
        Class<?> reasonC = Names.cls("EntitySpawnReason");
        if (reasonC != null) {
            spawnReason = Names.field("EntitySpawnReason.COMMAND", reasonC);
            if (spawnReason == null) spawnReason = Names.field("EntitySpawnReason.EVENT", reasonC);
        }
        if (createSeat1 == null && createSeat2 == null) problems.add("EntityType.create");

        // --- 1.19.3+ registry lookup (byString was removed in 26.x) ---
        Class<?> builtIn = Names.cls("BuiltInRegistries");
        Field regField = builtIn != null ? Names.field("BuiltInRegistries.ENTITY_TYPE", builtIn) : null;
        if (regField == null) {
            Class<?> registryC = Names.cls("Registry");
            regField = registryC != null ? Names.field("Registry.ENTITY_TYPE", registryC) : null;
        }
        if (regField != null) entityTypeRegistryObj = Names.get(regField, null);
        if (entityByString == null && entityTypeRegistryObj == null) {
            problems.add("entity type lookup");
        }

        // --- permissions ---
        Class<?> serverPlayerC = Names.cls("ServerPlayer");
        createSourceStack = Names.method("Entity.createCommandSourceStack", serverPlayerC != null ? serverPlayerC : entityC, 0);
        Class<?> cssC = Names.cls("CommandSourceStack");
        hasPermissionInt = cssC == null ? null : Names.method("CommandSourceStack.hasPermission", cssC, 1);
        if (hasPermissionInt != null && hasPermissionInt.getParameterTypes()[0] != int.class) hasPermissionInt = null;
        Class<?> permSetC = Names.cls("PermissionSet");
        if (permSetC != null && cssC != null) {
            permissionsM = Names.method("CommandSourceStack.permissions", cssC, 0);
            if (permissionsM == null) permissionsM = Names.methodByShape(cssC, permSetC, 0);
            Class<?> permsC = Names.cls("Permissions");
            gamemasterPerm = permsC == null ? null : Names.field("Permissions.COMMANDS_GAMEMASTER", permsC);
            permSetHas = Names.method("PermissionSet.hasPermission", permSetC, 1);
            if (permSetHas == null && gamemasterPerm != null) {
                permSetHas = Names.methodByShape(permSetC, boolean.class, 1, gamemasterPerm.getType());
            }
        }
    }

    // ------------------------------------------------------------------ reading

    public boolean isClientSide(Object level) {
        if (isClientSideM != null) return (Boolean) Names.invoke(isClientSideM, level);
        if (isClientSideF != null) return (Boolean) Names.get(isClientSideF, level);
        return false;
    }

    public State stateAt(Object level, Object pos) {
        Object raw = Names.invoke(getBlockState, level, pos);
        String text = String.valueOf(raw);
        Matcher m = STATE.matcher(text);
        String id = "";
        Map<String, String> props = new LinkedHashMap<>();
        if (m.find()) {
            id = m.group(1);
            if (m.group(2) != null && !m.group(2).isEmpty()) {
                for (String pair : m.group(2).split(",")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0) props.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
                }
            }
        }
        return new State(raw, Names.invoke(getBlockOfState, raw), id, props);
    }

    public int[] posXYZ(Object pos) {
        return new int[]{(Integer) Names.invoke(vec3iX, pos), (Integer) Names.invoke(vec3iY, pos), (Integer) Names.invoke(vec3iZ, pos)};
    }

    public boolean isMainHand(Object hand) {
        return hand instanceof Enum && ((Enum<?>) hand).ordinal() == 0;
    }

    public boolean isSneaking(Object player) {
        return (Boolean) Names.invoke(isShiftKeyDown, player);
    }

    public boolean isPassenger(Object player) {
        return (Boolean) Names.invoke(isPassenger, player);
    }

    public boolean mainHandEmpty(Object player) {
        return (Boolean) Names.invoke(itemIsEmpty, Names.invoke(getMainHandItem, player));
    }

    public float yaw(Object entity) {
        if (getYRotM != null) return ((Number) Names.invoke(getYRotM, entity)).floatValue();
        return ((Number) Names.get(yRotF, entity)).floatValue();
    }

    // ------------------------------------------------------------------ permissions

    /** Vanilla operator check (permission level 2), across the 1.21.11 permission rework. */
    public boolean isOp(Object player) {
        try {
            if (createSourceStack == null) return false;
            Object source = Names.invoke(createSourceStack, player);
            if (hasPermissionInt != null) return (Boolean) Names.invoke(hasPermissionInt, source, 2);
            if (permissionsM != null && permSetHas != null && gamemasterPerm != null) {
                return (Boolean) Names.invoke(permSetHas, Names.invoke(permissionsM, source), Names.get(gamemasterPerm, null));
            }
        } catch (RuntimeException e) {
            SitMod.LOG.fine("op check failed: " + e);
        }
        return false;
    }

    /** Operator check on a command source (what Brigadier hands to {@code requires}). */
    public boolean isOpSource(Object source) {
        try {
            if (hasPermissionInt != null) return (Boolean) Names.invoke(hasPermissionInt, source, 2);
            if (permissionsM != null && permSetHas != null && gamemasterPerm != null) {
                return (Boolean) Names.invoke(permSetHas, Names.invoke(permissionsM, source), Names.get(gamemasterPerm, null));
            }
        } catch (RuntimeException e) {
            SitMod.LOG.fine("op check failed: " + e);
        }
        return false;
    }

    /** Sends a chat line back to a command source, across the sendSuccess / sendSystemMessage API changes. */
    public void sendFeedback(Object source, String text) {
        try {
            Object component = component(text);
            if (component == null) return;
            Class<?> cssC = source.getClass();
            for (Method m : Names.publicMethods(cssC)) {
                Class<?>[] p = m.getParameterTypes();
                if (java.util.Arrays.asList(Names.names("M:CommandSourceStack.sendSuccess2")).contains(m.getName()) && p.length == 2 && p[1] == boolean.class) {
                    if (p[0] == java.util.function.Supplier.class) {
                        final Object c = component;
                        java.util.function.Supplier<Object> supplier = () -> c;
                        m.invoke(source, supplier, false);
                    } else {
                        m.invoke(source, component, false);
                    }
                    return;
                }
            }
            Method direct = Names.method("CommandSourceStack.sendSystemMessage", cssC, 1);
            if (direct != null) direct.invoke(source, component);
        } catch (ReflectiveOperationException | RuntimeException e) {
            SitMod.LOG.fine("could not send feedback: " + e);
        }
    }

    private Object component(String text) throws ReflectiveOperationException {
        Class<?> componentC = Names.cls("Component");
        Method literal = componentC == null ? null : Names.method("Component.literal", componentC, 1);
        if (literal != null && Modifier.isStatic(literal.getModifiers())) return literal.invoke(null, text);
        Class<?> textC = Names.cls("TextComponent");
        if (textC != null) return textC.getConstructor(String.class).newInstance(text);
        return null;
    }

    // ------------------------------------------------------------------ seat entity

    private Object entityType(String path) {
        Object cached = entityTypes.get(path);
        if (cached != null) return cached;
        Object type = null;
        String id = path.contains(":") ? path : "minecraft:" + path;
        if (entityByString != null) {
            try {
                Object r = Names.invoke(entityByString, null, id);
                if (r instanceof Optional) type = ((Optional<?>) r).orElse(null);
                else type = r;
            } catch (RuntimeException ignored) {
            }
        }
        if (type == null && entityTypeRegistryObj != null) type = lookupInRegistry(id);
        if (type != null) entityTypes.put(path, type);
        return type;
    }

    /**
     * Looks the entity type up in the registry. The key class is taken from the registry's own method signature
     * (ResourceLocation / Identifier), because some loaders ship legacy shims with the old class name.
     */
    private Object lookupInRegistry(String id) {
        List<String> keyNames = java.util.Arrays.asList(Names.names("C:ResourceLocation"));
        for (String name : new String[]{"getValue", "getOptional", "get"}) {
            for (Method m : Names.publicMethods(entityTypeRegistryObj.getClass())) {
                if (!m.getName().equals(name) || m.getParameterTypes().length != 1) continue;
                Class<?> keyType = m.getParameterTypes()[0];
                if (!keyNames.contains(keyType.getName())) continue;
                try {
                    Object key = makeKey(keyType, id);
                    if (key == null) continue;
                    m.setAccessible(true);
                    Object r = m.invoke(entityTypeRegistryObj, key);
                    if (r instanceof Optional) r = ((Optional<?>) r).orElse(null);
                    if (r != null && entityTypeC.isInstance(r)) return r;
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
        }
        SitMod.debug("registry lookup failed for " + id);
        return null;
    }

    private static Object makeKey(Class<?> keyType, String id) throws ReflectiveOperationException {
        Method factory = null;
        for (Method f : Names.declared(keyType)) {
            if (Modifier.isStatic(f.getModifiers()) && f.getParameterTypes().length == 1 && f.getParameterTypes()[0] == String.class
                    && f.getReturnType() == keyType && (f.getName().equals("parse") || f.getName().equals("tryParse") || factory == null)) {
                f.setAccessible(true);
                factory = f;
            }
        }
        if (factory != null) return factory.invoke(null, id);
        Constructor<?> ctor = keyType.getConstructor(String.class);
        return ctor.newInstance(id);
    }

    /**
     * Creates, positions and flags a seat entity, adds it to the world and mounts the player on it.
     *
     * @return the seat entity, or null when anything failed
     */
    public Object spawnSeat(Object level, Object player, String typeName, double x, double y, double z, float yaw, String tag) {
        Object type = entityType(typeName.toLowerCase(java.util.Locale.ROOT));
        if (type == null) {
            SitMod.LOG.warning("Unknown entity type '" + typeName + "' in sitables config");
            return null;
        }
        Object seat;
        if (createSeat2 != null) {
            Object reason = spawnReason == null ? null : Names.get(spawnReason, null);
            seat = Names.invoke(createSeat2, type, level, reason);
        } else {
            seat = Names.invoke(createSeat1, type, level);
        }
        if (seat == null) {
            SitMod.debug("EntityType.create returned null for " + typeName);
            return null;
        }

        Names.invoke(moveTo, seat, x, y, z, yaw, 0.0f);
        Names.invoke(setNoGravity, seat, true);
        Names.invoke(setInvulnerable, seat, true);
        Names.invoke(setSilent, seat, true);
        Names.invoke(setInvisible, seat, true);
        if (mobC != null && mobC.isInstance(seat) && setNoAi != null) Names.invoke(setNoAi, seat, true);
        if (addTag != null) Names.invoke(addTag, seat, tag);

        Names.invoke(addFreshEntity, level, seat);
        boolean mounted;
        if (startRiding2 != null) mounted = (Boolean) Names.invoke(startRiding2, player, seat, true);
        else if (startRiding3 != null) mounted = (Boolean) Names.invoke(startRiding3, player, seat, true, false);
        else mounted = (Boolean) Names.invoke(startRiding1, player, seat);
        if (!mounted) {
            SitMod.debug("startRiding returned false, the seat is discarded");
            discard(seat);
            return null;
        }
        return seat;
    }

    public boolean hasTag(Object entity, String tag) {
        if (getTags == null) return false;
        Object tags = Names.invoke(getTags, entity);
        return tags instanceof java.util.Collection && ((java.util.Collection<?>) tags).contains(tag);
    }

    public boolean isVehicle(Object entity) {
        return (Boolean) Names.invoke(isVehicle, entity);
    }

    public boolean isRemoved(Object entity) {
        if (isRemoved != null) return (Boolean) Names.invoke(isRemoved, entity);
        if (removedF != null) return (Boolean) Names.get(removedF, entity);
        return false;
    }

    public void discard(Object entity) {
        Names.invoke(discard != null ? discard : remove0, entity);
    }
}
