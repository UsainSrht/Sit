package me.usainsrht.sit.mod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small YAML reader for the subset {@code config.yml} uses: nested block maps, block lists, quoted and plain scalars,
 * comments, and empty / one line flow collections ({@code []}, {@code {}}, {@code [a, b]}). It replaces SnakeYAML in the
 * mod build to keep the jar small. Anything it does not understand throws {@link IllegalArgumentException}, and the
 * caller falls back to the bundled default config.
 * <p>
 * Results use the same shapes SnakeYAML produces: {@link LinkedHashMap} (order kept), {@link ArrayList}, {@link Boolean},
 * {@link Integer} / {@link Long} / {@link Double}, {@link String} and null.
 */
final class MiniYaml {

    private final List<String> text = new ArrayList<>();
    private final List<Integer> indent = new ArrayList<>();
    private int pos;

    private MiniYaml(String source) {
        for (String raw : source.split("\r?\n|\r", -1)) {
            if (raw.indexOf('\t') >= 0 && raw.substring(0, raw.length() - raw.replaceAll("^\\s+", "").length()).indexOf('\t') >= 0) {
                throw new IllegalArgumentException("tabs are not allowed for indentation");
            }
            String line = stripComment(raw);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.equals("---")) continue;
            int spaces = 0;
            while (spaces < line.length() && line.charAt(spaces) == ' ') spaces++;
            text.add(trimmed);
            indent.add(spaces);
        }
    }

    static Object parse(String source) {
        MiniYaml y = new MiniYaml(source);
        if (y.text.isEmpty()) return null;
        Object result = y.block(y.indent.get(0));
        if (y.pos < y.text.size()) throw y.error("unexpected indentation");
        return result;
    }

    // ------------------------------------------------------------------ blocks

    private Object block(int ind) {
        return isListItem(text.get(pos)) ? list(ind) : map(ind);
    }

    private static boolean isListItem(String s) {
        return s.equals("-") || s.startsWith("- ");
    }

    private Map<String, Object> map(int ind) {
        Map<String, Object> out = new LinkedHashMap<>();
        while (pos < text.size() && indent.get(pos) == ind && !isListItem(text.get(pos))) {
            String line = text.get(pos);
            int colon = keyEnd(line);
            if (colon < 0) throw error("expected 'key: value'");
            String key = unquote(line.substring(0, colon).trim());
            String rest = line.substring(colon + 1).trim();
            pos++;
            Object value;
            if (rest.isEmpty()) {
                if (pos < text.size() && indent.get(pos) > ind) value = block(indent.get(pos));
                else if (pos < text.size() && indent.get(pos) == ind && isListItem(text.get(pos))) value = list(ind); // "key:\n- a"
                else value = null;
            } else {
                value = inline(rest);
            }
            out.put(key, value);
        }
        if (pos < text.size() && indent.get(pos) > ind) throw error("unexpected indentation");
        return out;
    }

    private List<Object> list(int ind) {
        List<Object> out = new ArrayList<>();
        while (pos < text.size() && indent.get(pos) == ind && isListItem(text.get(pos))) {
            String rest = text.get(pos).substring(1).trim();
            if (rest.isEmpty()) {
                pos++;
                out.add(pos < text.size() && indent.get(pos) > ind ? block(indent.get(pos)) : null);
            } else if (keyEnd(rest) >= 0 && !rest.startsWith("\"") && !rest.startsWith("'") && !rest.startsWith("[") && !rest.startsWith("{")) {
                // "- key: value" starts a map whose keys line up after the dash
                text.set(pos, rest);
                indent.set(pos, ind + 2);
                out.add(map(ind + 2));
            } else {
                pos++;
                out.add(inline(rest));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ scalars and flow collections

    private Object inline(String s) {
        if (s.startsWith("[") || s.startsWith("{")) return flow(s);
        if (s.startsWith("|") || s.startsWith(">") || s.startsWith("&") || s.startsWith("*") || s.startsWith("!")) {
            throw error("block scalars, anchors and tags are not supported");
        }
        return scalar(s);
    }

    private Object flow(String s) {
        int[] at = {0};
        Object v = flowValue(s, at);
        skipSpaces(s, at);
        if (at[0] != s.length()) throw error("unexpected text after flow collection");
        return v;
    }

    private Object flowValue(String s, int[] at) {
        skipSpaces(s, at);
        if (at[0] >= s.length()) throw error("unterminated flow collection");
        char c = s.charAt(at[0]);
        if (c == '[') {
            at[0]++;
            List<Object> out = new ArrayList<>();
            skipSpaces(s, at);
            if (at[0] < s.length() && s.charAt(at[0]) == ']') {
                at[0]++;
                return out;
            }
            for (;;) {
                out.add(flowValue(s, at));
                skipSpaces(s, at);
                if (at[0] >= s.length()) throw error("unterminated flow list");
                char d = s.charAt(at[0]++);
                if (d == ']') return out;
                if (d != ',') throw error("expected ',' or ']'");
            }
        }
        if (c == '{') {
            at[0]++;
            Map<String, Object> out = new LinkedHashMap<>();
            skipSpaces(s, at);
            if (at[0] < s.length() && s.charAt(at[0]) == '}') {
                at[0]++;
                return out;
            }
            for (;;) {
                String token = flowToken(s, at, ":");
                skipSpaces(s, at);
                if (at[0] >= s.length() || s.charAt(at[0]) != ':') throw error("expected ':' in flow map");
                at[0]++;
                out.put(unquote(token.trim()), flowValue(s, at));
                skipSpaces(s, at);
                if (at[0] >= s.length()) throw error("unterminated flow map");
                char d = s.charAt(at[0]++);
                if (d == '}') return out;
                if (d != ',') throw error("expected ',' or '}'");
            }
        }
        return scalar(flowToken(s, at, ",]}").trim());
    }

    private String flowToken(String s, int[] at, String stops) {
        skipSpaces(s, at);
        int start = at[0];
        if (start < s.length() && (s.charAt(start) == '"' || s.charAt(start) == '\'')) {
            char q = s.charAt(start);
            int i = start + 1;
            while (i < s.length() && s.charAt(i) != q) {
                if (q == '"' && s.charAt(i) == '\\') i++;
                i++;
            }
            if (i >= s.length()) throw error("unterminated quoted string");
            at[0] = i + 1;
            return s.substring(start, at[0]);
        }
        int i = start;
        while (i < s.length() && stops.indexOf(s.charAt(i)) < 0) i++;
        at[0] = i;
        return s.substring(start, i);
    }

    private static void skipSpaces(String s, int[] at) {
        while (at[0] < s.length() && s.charAt(at[0]) == ' ') at[0]++;
    }

    private Object scalar(String s) {
        if (s.isEmpty()) return null;
        char c = s.charAt(0);
        if (c == '"' || c == '\'') return unquote(s);
        switch (s) {
            case "~": case "null": case "Null": case "NULL": return null;
            case "true": case "True": case "TRUE": case "yes": case "Yes": case "YES": case "on": case "On": case "ON": return Boolean.TRUE;
            case "false": case "False": case "FALSE": case "no": case "No": case "NO": case "off": case "Off": case "OFF": return Boolean.FALSE;
            default: break;
        }
        if (s.matches("[-+]?\\d+")) {
            try {
                long l = Long.parseLong(s.startsWith("+") ? s.substring(1) : s);
                return l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE ? (Object) (int) l : (Object) l;
            } catch (NumberFormatException ignored) {
                return s;
            }
        }
        if (s.matches("[-+]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][-+]?\\d+)?")) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return s;
            }
        }
        return s;
    }

    private String unquote(String s) {
        if (s.length() >= 2 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') {
            return s.substring(1, s.length() - 1).replace("''", "'");
        }
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < s.length() - 1; i++) {
                char c = s.charAt(i);
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char n = s.charAt(++i);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '0': sb.append('\0'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                        i += 4;
                        break;
                    default: throw error("unsupported escape \\" + n);
                }
            }
            return sb.toString();
        }
        return s;
    }

    // ------------------------------------------------------------------ helpers

    /** Index of the colon ending a mapping key (followed by a space or the end of the line), outside quotes. */
    private static int keyEnd(String s) {
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') {
                if (i == 0) quote = c;
            } else if (c == ':' && (i + 1 == s.length() || s.charAt(i + 1) == ' ')) {
                return i;
            }
        }
        return -1;
    }

    /** Removes a trailing {@code # comment} that sits outside quotes. */
    private static String stripComment(String line) {
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote != 0) {
                if (c == '\\' && quote == '"') i++;
                else if (c == quote) quote = 0;
            } else if ((c == '"' || c == '\'') && (i == 0 || " [{,:-".indexOf(line.charAt(i - 1)) >= 0)) {
                quote = c;
            } else if (c == '#' && (i == 0 || line.charAt(i - 1) == ' ' || line.charAt(i - 1) == '\t')) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("config.yml, near line item " + (pos + 1) + ": " + message);
    }
}
