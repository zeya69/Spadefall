package com.supercraftmc.spadefall.map.schematic;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * A minimal NBT reader - enough to load a Sponge schematic. Bukkit does not
 * expose an NBT API, and pulling in a library for one file format would be
 * the wrong trade.
 *
 * Compounds become Map<String,Object>, lists become List<Object>, arrays
 * become their primitive array types, scalars are boxed.
 */
public final class Nbt {

    private Nbt() { }

    public static Map<String, Object> readGzipped(InputStream in) throws IOException {
        try (DataInputStream data = new DataInputStream(new GZIPInputStream(in))) {
            int type = data.readUnsignedByte();
            if (type != 10) {
                throw new IOException("Root tag is not a compound (type " + type + ")");
            }
            readString(data); // root name, ignored
            return readCompound(data);
        }
    }

    private static Map<String, Object> readCompound(DataInputStream in) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        while (true) {
            int type = in.readUnsignedByte();
            if (type == 0) {
                return map;
            }
            String name = readString(in);
            map.put(name, readPayload(in, type));
        }
    }

    private static Object readPayload(DataInputStream in, int type) throws IOException {
        switch (type) {
            case 1: return in.readByte();
            case 2: return in.readShort();
            case 3: return in.readInt();
            case 4: return in.readLong();
            case 5: return in.readFloat();
            case 6: return in.readDouble();
            case 7: {
                int n = in.readInt();
                byte[] out = new byte[n];
                in.readFully(out);
                return out;
            }
            case 8: return readString(in);
            case 9: {
                int elementType = in.readUnsignedByte();
                int n = in.readInt();
                List<Object> list = new ArrayList<>(Math.max(0, n));
                for (int i = 0; i < n; i++) {
                    list.add(readPayload(in, elementType));
                }
                return list;
            }
            case 10: return readCompound(in);
            case 11: {
                int n = in.readInt();
                int[] out = new int[n];
                for (int i = 0; i < n; i++) out[i] = in.readInt();
                return out;
            }
            case 12: {
                int n = in.readInt();
                long[] out = new long[n];
                for (int i = 0; i < n; i++) out[i] = in.readLong();
                return out;
            }
            default:
                throw new IOException("Unknown NBT tag type " + type);
        }
    }

    private static String readString(DataInputStream in) throws IOException {
        int len = in.readUnsignedShort();
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    // ---- typed accessors ---------------------------------------------------

    public static int getInt(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    public static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof String s ? s : null;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getCompound(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getList(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof List ? (List<Object>) v : null;
    }
}
