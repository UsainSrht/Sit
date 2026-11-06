package me.usainsrht.sit.mod;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Reads the exact same {@code config.yml} the Bukkit plugin uses (same keys, same order, same semantics),
 * so a server can move between the plugin and the mod without touching its configuration.
 */
public final class SitConfig {

    /** One entry of {@code sitables}. */
    public static final class Layout {
        public final String name;
        public final boolean permissionRequired;
        public final String permissionName;
        public final String entityType;
        public final boolean saddle;
        public final double x, y, z;
        public final String check;
        public final List<String> list;

        Layout(String name, Map<?, ?> data) {
            this.name = name;
            this.permissionRequired = bool(get(data, "permission", "require"));
            this.permissionName = str(get(data, "permission", "name"));
            this.entityType = str(get(data, "entity", "type"));
            this.saddle = bool(get(data, "entity", "saddle"));
            this.x = num(get(data, "offsets", "x"));
            this.y = num(get(data, "offsets", "y"));
            this.z = num(get(data, "offsets", "z"));
            this.check = str(get(data, "check"));
            List<String> items = new ArrayList<>();
            Object raw = get(data, "list");
            if (raw instanceof List) for (Object o : (List<?>) raw) if (o != null) items.add(String.valueOf(o));
            this.list = Collections.unmodifiableList(items);
        }
    }

    private final List<Layout> layouts;

    private SitConfig(List<Layout> layouts) {
        this.layouts = layouts;
    }

    public List<Layout> layouts() {
        return layouts;
    }

    /** Bundled default config, at a path unique to this mod (a bare /config.yml could resolve to another mod's file). */
    private static final String DEFAULT_RESOURCE = "/sit/config.yml";

    /** Loads {@code <dir>/config.yml}, writing the bundled default first when the file does not exist. */
    public static SitConfig load(File dir) {
        File file = new File(dir, "config.yml");
        try {
            if (!file.exists()) {
                Files.createDirectories(dir.toPath());
                try (InputStream in = SitConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
                    if (in != null) Files.copy(in, file.toPath());
                }
            }
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                return parse(MiniYaml.parse(readAll(reader)));
            }
        } catch (IOException | RuntimeException e) {
            SitMod.LOG.warning("Could not read " + file + ": " + e);
            try (InputStream in = SitConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
                if (in != null) return parse(MiniYaml.parse(readAll(new java.io.InputStreamReader(in, StandardCharsets.UTF_8))));
            } catch (IOException | RuntimeException ignored) {
            }
            return new SitConfig(Collections.<Layout>emptyList());
        }
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        for (int n; (n = reader.read(buf)) > 0; ) sb.append(buf, 0, n);
        return sb.toString();
    }

    private static SitConfig parse(Object root) {
        List<Layout> out = new ArrayList<>();
        Object sitables = root instanceof Map ? ((Map<?, ?>) root).get("sitables") : null;
        if (sitables instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) sitables).entrySet()) {
                if (e.getValue() instanceof Map) out.add(new Layout(String.valueOf(e.getKey()), (Map<?, ?>) e.getValue()));
            }
        }
        return new SitConfig(Collections.unmodifiableList(out));
    }

    private static Object get(Map<?, ?> map, String... path) {
        Object cur = map;
        for (String p : path) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<?, ?>) cur).get(p);
        }
        return cur;
    }

    private static boolean bool(Object o) {
        return o instanceof Boolean ? (Boolean) o : o != null && Boolean.parseBoolean(String.valueOf(o));
    }

    private static double num(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return o == null ? 0 : Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

}
