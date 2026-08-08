package com.supercraftmc.spadefall.config;

import com.supercraftmc.spadefall.SpadefallPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads messages.yml and renders legacy colour codes.
 *
 * Deliberately not using Adventure components here: the message file is
 * owner-editable and ampersand codes are what plugin owners expect to type.
 */
public final class Messages {

    private final SpadefallPlugin plugin;
    private FileConfiguration config;
    private String prefix = "";

    public Messages(SpadefallPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        prefix = colour(config.getString("prefix", ""));
    }

    public String raw(String path) {
        String value = config.getString(path);
        if (value == null) {
            plugin.getLogger().warning("Missing message key: " + path);
            return path;
        }
        return colour(value);
    }

    public String get(String path, Object... replacements) {
        String value = raw(path);
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Replacements must be key/value pairs");
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < replacements.length; i += 2) {
            map.put(String.valueOf(replacements[i]), String.valueOf(replacements[i + 1]));
        }
        for (Map.Entry<String, String> e : map.entrySet()) {
            value = value.replace("%" + e.getKey() + "%", e.getValue());
        }
        return value;
    }

    public void send(CommandSender to, String path, Object... replacements) {
        to.sendMessage(prefix + get(path, replacements));
    }

    /** Send without the prefix - for multi-line blocks like validation reports. */
    public void sendBare(CommandSender to, String path, Object... replacements) {
        to.sendMessage(get(path, replacements));
    }

    public String getPrefix() {
        return prefix;
    }

    public static String colour(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length());
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            // &#RRGGBB -> the section-x form Bukkit understands
            if (chars[i] == '&' && i + 7 < chars.length && chars[i + 1] == '#'
                    && isHex(chars, i + 2, 6)) {
                out.append('\u00A7').append('x');
                for (int j = i + 2; j < i + 8; j++) {
                    out.append('\u00A7').append(chars[j]);
                }
                i += 7;
                continue;
            }
            if (chars[i] == '&' && i + 1 < chars.length
                    && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1) {
                out.append('\u00A7').append(Character.toLowerCase(chars[i + 1]));
                i++;
                continue;
            }
            out.append(chars[i]);
        }
        return out.toString();
    }

    private static boolean isHex(char[] chars, int from, int length) {
        for (int i = from; i < from + length; i++) {
            if (Character.digit(chars[i], 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
