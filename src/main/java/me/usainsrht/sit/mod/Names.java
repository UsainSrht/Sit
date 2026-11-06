package me.usainsrht.sit.mod;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves Minecraft classes, methods and fields by their official (Mojang) names across every
 * mapping scheme the game can run in (Mojang names, Fabric intermediary, Forge SRG, 1.16 MCP names).
 * <p>
 * {@code sit-mappings.txt} is generated at build time by {@code tools/gen-mappings.js} from the real mapping files
 * of every supported Minecraft version. Each entry lists every name a member has had, in any scheme.
 */
public final class Names {

    private static final Map<String, String[]> TABLE = new HashMap<>();
    private static final ClassLoader LOADER = Names.class.getClassLoader();

    static {
        try (InputStream in = Names.class.getResourceAsStream("/sit-mappings.txt")) {
            if (in != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    int eq = line.indexOf('=');
                    if (eq > 0) TABLE.put(line.substring(0, eq), line.substring(eq + 1).split(","));
                }
            }
        } catch (Exception e) {
            SitMod.LOG.warning("Could not load sit-mappings.txt: " + e);
        }
    }

    private Names() {}

    public static String[] names(String key) {
        String[] names = TABLE.get(key);
        return names == null ? new String[0] : names;
    }

    private static final Map<String, Class<?>> CLASSES = new HashMap<>();

    /** Finds a class by key (e.g. {@code Entity}) or null when it does not exist in this version. */
    public static synchronized Class<?> cls(String key) {
        if (CLASSES.containsKey(key)) return CLASSES.get(key);
        Class<?> found = null;
        for (String name : names("C:" + key)) {
            found = load(name);
            if (found != null) break;
        }
        CLASSES.put(key, found);
        return found;
    }

    public static Class<?> load(String name) {
        try {
            return Class.forName(name, false, LOADER);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    /** Finds the first public/inherited method named by any alias of {@code key} with {@code argc} parameters. */
    public static Method method(String key, Class<?> owner, int argc) {
        return method(key, owner, argc, null);
    }

    /** Like {@link #method(String, Class, int)} but only accepts methods the filter likes (overloads share names). */
    public static Method method(String key, Class<?> owner, int argc, java.util.function.Predicate<Method> filter) {
        if (owner == null) return null;
        Set<String> aliases = new LinkedHashSet<>();
        for (String n : names("M:" + key)) aliases.add(n);
        for (String alias : aliases) {
            for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
                Method m = scan(declared(c), alias, argc, filter);
                if (m != null) return m;
                for (Class<?> i : c.getInterfaces()) {
                    m = scanInterface(i, alias, argc, filter);
                    if (m != null) return m;
                }
            }
            if (owner.isInterface()) {
                Method m = scanInterface(owner, alias, argc, filter);
                if (m != null) return m;
            }
        }
        return null;
    }

    /** getDeclaredMethods resolves every signature type, which can hit client-only classes on a dedicated server. */
    static Method[] declared(Class<?> c) {
        try {
            return c.getDeclaredMethods();
        } catch (LinkageError e) {
            return new Method[0];
        }
    }

    static Method[] publicMethods(Class<?> c) {
        try {
            return c.getMethods();
        } catch (LinkageError e) {
            return new Method[0];
        }
    }

    private static Method scanInterface(Class<?> type, String alias, int argc, java.util.function.Predicate<Method> filter) {
        Method m = scan(declared(type), alias, argc, filter);
        if (m != null) return m;
        for (Class<?> i : type.getInterfaces()) {
            m = scanInterface(i, alias, argc, filter);
            if (m != null) return m;
        }
        return null;
    }

    private static Method scan(Method[] methods, String name, int argc, java.util.function.Predicate<Method> filter) {
        for (Method m : methods) {
            if (m.getName().equals(name) && m.getParameterTypes().length == argc && !m.isBridge() && (filter == null || filter.test(m))) {
                try {
                    m.setAccessible(true);
                } catch (RuntimeException ignored) {
                }
                return m;
            }
        }
        return null;
    }

    /** Finds a method by shape instead of name: used when a mapping is missing (e.g. a brand-new API). */
    public static Method methodByShape(Class<?> owner, Class<?> returnType, int argc, Class<?>... paramTypes) {
        if (owner == null || returnType == null) return null;
        for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
            List<Method> all = new ArrayList<>();
            for (Method m : declared(c)) all.add(m);
            for (Class<?> i : c.getInterfaces()) for (Method m : publicMethods(i)) all.add(m);
            for (Method m : all) {
                if (m.isBridge() || Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterTypes().length != argc || !returnType.isAssignableFrom(m.getReturnType())) continue;
                boolean ok = true;
                for (int i = 0; i < paramTypes.length && ok; i++) ok = paramTypes[i].isAssignableFrom(m.getParameterTypes()[i]);
                if (ok) {
                    try {
                        m.setAccessible(true);
                    } catch (RuntimeException ignored) {
                    }
                    return m;
                }
            }
        }
        return null;
    }

    public static Field field(String key, Class<?> owner) {
        if (owner == null) return null;
        for (String alias : names("F:" + key)) {
            for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
                Field[] fields;
                try {
                    fields = c.getDeclaredFields();
                } catch (LinkageError e) {
                    continue;
                }
                for (Field f : fields) {
                    if (f.getName().equals(alias)) {
                        try {
                            f.setAccessible(true);
                        } catch (RuntimeException ignored) {
                        }
                        return f;
                    }
                }
            }
        }
        return null;
    }

    public static Object invoke(Method m, Object target, Object... args) {
        try {
            return m.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new IllegalStateException(m + " failed", e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(m + " not accessible", e);
        }
    }

    public static Object get(Field f, Object target) {
        try {
            return f.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(f + " not accessible", e);
        }
    }

    public static String describe(Member m) {
        return m == null ? "<missing>" : m.getDeclaringClass().getName() + "#" + m.getName();
    }
}
