package me.usainsrht.sit.mod;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Event wiring for each mod loader. Loader APIs (unlike Minecraft itself) keep their names, but their shape moves
 * around between versions (EventBus 6 vs 7, renamed events), so they are bound reflectively as well.
 */
public final class Loaders {

    private Loaders() {}

    // ------------------------------------------------------------------ Fabric

    public static void fabric() {
        SitMod.init("Fabric");
        if (!SitMod.isReady()) return;
        try {
            Class<?> callback = Names.load("net.fabricmc.fabric.api.event.player.UseBlockCallback");
            if (callback == null) {
                SitMod.LOG.warning("Fabric API is not installed, Sit cannot hook block interaction");
                return;
            }
            register(callback, "EVENT", callback, (proxy, method, args) -> {
                // interact(Player, Level, InteractionHand, BlockHitResult) -> InteractionResult
                Class<?> result = method.getReturnType();
                boolean handled = false;
                try {
                    Method blockPos = Names.method("BlockHitResult.getBlockPos", args[3].getClass(), 0);
                    handled = SitMod.onUse(args[0], args[1], Names.invoke(blockPos, args[3]), args[2]);
                } catch (RuntimeException e) {
                    SitMod.LOG.warning("Sit interaction hook failed: " + e);
                }
                Field f = Names.field("InteractionResult." + (handled ? "SUCCESS" : "PASS"), result);
                return f == null ? null : Names.get(f, null);
            });

            Class<?> endTick = Names.load("net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents$EndTick");
            Class<?> tickEvents = Names.load("net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents");
            if (endTick != null && tickEvents != null) {
                register(tickEvents, "END_SERVER_TICK", endTick, (proxy, method, args) -> {
                    SitMod.tick();
                    return null;
                });
            } else {
                SitMod.LOG.warning("Sit could not find Fabric's server tick event (" + endTick + ", " + tickEvents + "), seats will not be cleaned up");
            }
            Class<?> load = Names.load("net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents$Load");
            Class<?> entityEvents = Names.load("net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents");
            if (load != null && entityEvents != null) {
                register(entityEvents, "ENTITY_LOAD", load, (proxy, method, args) -> {
                    SitMod.onEntityLoaded(args[0]);
                    return null;
                });
            }
            SitCommands.registerFabric();
        } catch (ReflectiveOperationException | RuntimeException e) {
            SitMod.LOG.severe("Sit failed to register Fabric events: " + e);
        }
    }

    private static void register(Class<?> holder, String field, Class<?> listenerType, InvocationHandler handler) throws ReflectiveOperationException {
        Object event = holder.getField(field).get(null);
        Object proxy = Proxy.newProxyInstance(listenerType.getClassLoader(), new Class<?>[]{listenerType}, (p, m, a) -> {
            if (m.getDeclaringClass() == Object.class) {
                switch (m.getName()) {
                    case "hashCode": return System.identityHashCode(p);
                    case "equals": return p == a[0];
                    default: return "SitListener";
                }
            }
            return handler.invoke(p, m, a);
        });
        invokeOneArg(event, "register", proxy);
    }

    // ------------------------------------------------------------------ Forge / NeoForge

    public static void forge() {
        forgeLike("Forge", "net.minecraftforge.common.MinecraftForge", "EVENT_BUS",
                "net.minecraftforge.eventbus.api.EventPriority",
                new String[]{"net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickBlock"},
                new String[]{"net.minecraftforge.event.TickEvent$ServerTickEvent$Post", "net.minecraftforge.event.TickEvent$ServerTickEvent"},
                "net.minecraftforge.event.RegisterCommandsEvent",
                new String[]{"net.minecraftforge.event.entity.EntityJoinLevelEvent", "net.minecraftforge.event.entity.EntityJoinWorldEvent"});
    }

    public static void neoForge() {
        forgeLike("NeoForge", "net.neoforged.neoforge.common.NeoForge", "EVENT_BUS",
                "net.neoforged.bus.api.EventPriority",
                new String[]{"net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickBlock"},
                new String[]{"net.neoforged.neoforge.event.tick.ServerTickEvent$Post", "net.neoforged.neoforge.event.TickEvent$ServerTickEvent"},
                "net.neoforged.neoforge.event.RegisterCommandsEvent",
                new String[]{"net.neoforged.neoforge.event.entity.EntityJoinLevelEvent"});
    }

    private static void forgeLike(String name, String busHolder, String busField, String priorityClass,
                                  String[] rightClickClasses, String[] tickClasses, String commandsEvent, String[] joinClasses) {
        SitMod.init(name);
        if (!SitMod.isReady()) return;
        try {
            Class<?> click = firstClass(rightClickClasses);
            if (click == null) {
                SitMod.LOG.severe("Sit could not find the " + name + " right click event");
                return;
            }
            listen(busHolder, busField, priorityClass, click, event -> {
                Object player = call(event, "getEntity", "getPlayer");
                Object level = call(event, "getLevel", "getWorld");
                Object pos = call(event, "getPos");
                Object hand = call(event, "getHand");
                if (player == null || level == null || pos == null || hand == null) return false;
                boolean handled = SitMod.onUse(player, level, pos, hand);
                if (handled) {
                    try {
                        for (Method m : Names.publicMethods(event.getClass())) {
                            if (m.getName().equals("setCancellationResult") && m.getParameterTypes().length == 1) {
                                Field f = Names.field("InteractionResult.SUCCESS", m.getParameterTypes()[0]);
                                if (f != null) m.invoke(event, Names.get(f, null));
                            }
                        }
                        for (Method m : Names.publicMethods(event.getClass())) {
                            if (m.getName().equals("setCanceled") && m.getParameterTypes().length == 1) m.invoke(event, true);
                        }
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                    }
                }
                return handled;
            });

            Class<?> tick = firstClass(tickClasses);
            if (tick != null) {
                listen(busHolder, busField, priorityClass, tick, event -> {
                    SitMod.tick();
                    return false;
                });
            } else {
                SitMod.LOG.warning("Sit could not find a server tick event, seats will not be cleaned up");
            }
            Class<?> join = firstClass(joinClasses);
            if (join != null) {
                listen(busHolder, busField, priorityClass, join, event -> {
                    SitMod.onEntityLoaded(call(event, "getEntity"));
                    return false;
                });
            }
            SitCommands.registerForgeLike(busHolder, busField, priorityClass, commandsEvent);
        } catch (ReflectiveOperationException | RuntimeException e) {
            SitMod.LOG.severe("Sit failed to register " + name + " events: " + e);
        }
    }

    /** Registers a listener on the loader's event bus; the handler returns true to cancel (EventBus 7 only). */
    static void listen(String busHolder, String busField, String priorityClass, Class<?> eventClass, Predicate<Object> handler)
            throws ReflectiveOperationException {
        // EventBus 7 (Forge 1.21.6+): every event class carries its own static BUS.
        try {
            Field bus = eventClass.getField("BUS");
            Object eventBus = bus.get(null);
            for (Method m : Names.publicMethods(eventBus.getClass())) {
                if (m.getName().equals("addListener") && m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == Predicate.class) {
                    m.invoke(eventBus, handler);
                    return;
                }
            }
            for (Method m : Names.publicMethods(eventBus.getClass())) {
                if (m.getName().equals("addListener") && m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == Consumer.class) {
                    Consumer<Object> consumer = handler::test;
                    m.invoke(eventBus, consumer);
                    return;
                }
            }
        } catch (NoSuchFieldException ignored) {
            // classic bus below
        }
        Object eventBus = Class.forName(busHolder, true, Names.class.getClassLoader()).getField(busField).get(null);
        Class<?> priority = Class.forName(priorityClass, true, Names.class.getClassLoader());
        Object highest = null;
        for (Object c : priority.getEnumConstants()) if (c.toString().equals("HIGHEST")) highest = c;
        Consumer<Object> consumer = handler::test;
        for (Method m : Names.publicMethods(eventBus.getClass())) {
            Class<?>[] p = m.getParameterTypes();
            if (m.getName().equals("addListener") && p.length == 4 && p[0] == priority && p[1] == boolean.class && p[2] == Class.class) {
                m.invoke(eventBus, highest, false, eventClass, consumer);
                return;
            }
        }
        throw new NoSuchMethodException("IEventBus.addListener(priority, boolean, Class, Consumer)");
    }

    /** Calls a one argument method through a public declaring type (event impl classes are often package private). */
    static void invokeOneArg(Object target, String name, Object arg) throws ReflectiveOperationException {
        Method fallback = null;
        for (Method m : Names.publicMethods(target.getClass())) {
            if (m.getName().equals(name) && m.getParameterTypes().length == 1 && m.getParameterTypes()[0].isInstance(arg)) {
                if (java.lang.reflect.Modifier.isPublic(m.getDeclaringClass().getModifiers())) {
                    m.invoke(target, arg);
                    return;
                }
                fallback = m;
            }
        }
        if (fallback == null) throw new NoSuchMethodException(target.getClass().getName() + "." + name);
        fallback.setAccessible(true);
        fallback.invoke(target, arg);
    }

    static Class<?> firstClass(String... names) {
        for (String n : names) {
            Class<?> c = Names.load(n);
            if (c != null) return c;
        }
        return null;
    }

    static Object call(Object target, String... names) {
        for (String name : names) {
            try {
                Method m = target.getClass().getMethod(name);
                return m.invoke(target);
            } catch (NoSuchMethodException ignored) {
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
        return null;
    }
}
