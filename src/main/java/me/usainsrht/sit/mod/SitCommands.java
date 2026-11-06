package me.usainsrht.sit.mod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import java.lang.reflect.Proxy;

/** The {@code /sit reload} command, registered through Brigadier on every loader. */
@SuppressWarnings({"rawtypes", "unchecked"})
final class SitCommands {

    private SitCommands() {}

    static void register(Object dispatcherObject) {
        CommandDispatcher dispatcher = (CommandDispatcher) dispatcherObject;
        LiteralArgumentBuilder sit = LiteralArgumentBuilder.literal("sit")
                .requires(source -> SitMod.mc().isOpSource(source))
                .executes(ctx -> {
                    SitMod.mc().sendFeedback(ctx.getSource(), "/sit reload");
                    return 1;
                })
                .then(LiteralArgumentBuilder.literal("reload").executes(ctx -> {
                    SitMod.reload();
                    SitMod.mc().sendFeedback(ctx.getSource(), "§aconfig reloaded succesfully");
                    return 1;
                }));
        dispatcher.register(sit);
    }

    static void registerFabric() {
        for (String name : new String[]{"net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback",
                "net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback"}) {
            Class<?> callback = Names.load(name);
            if (callback == null) continue;
            try {
                Object event = callback.getField("EVENT").get(null);
                Object proxy = Proxy.newProxyInstance(callback.getClassLoader(), new Class<?>[]{callback}, (p, m, a) -> {
                    if (m.getDeclaringClass() == Object.class) return m.getName().equals("hashCode") ? System.identityHashCode(p) : m.getName().equals("equals") ? p == a[0] : "SitCommands";
                    try {
                        register(a[0]);
                    } catch (RuntimeException e) {
                        SitMod.LOG.warning("Could not register /sit: " + e);
                    }
                    return null;
                });
                Loaders.invokeOneArg(event, "register", proxy);
                return;
            } catch (ReflectiveOperationException | RuntimeException e) {
                SitMod.LOG.warning("Could not hook command registration: " + e);
            }
        }
    }

    static void registerForgeLike(String busHolder, String busField, String priorityClass, String eventClass) {
        Class<?> event = Names.load(eventClass);
        if (event == null) return;
        try {
            Loaders.listen(busHolder, busField, priorityClass, event, e -> {
                Object dispatcher = Loaders.call(e, "getDispatcher");
                if (dispatcher != null) {
                    try {
                        register(dispatcher);
                    } catch (RuntimeException ex) {
                        SitMod.LOG.warning("Could not register /sit: " + ex);
                    }
                }
                return false;
            });
        } catch (ReflectiveOperationException | RuntimeException e) {
            SitMod.LOG.warning("Could not hook command registration: " + e);
        }
    }
}
