package com.supercraftmc.spadefall.map.schematic;

import com.supercraftmc.spadefall.map.Marker;
import com.supercraftmc.spadefall.map.MarkerRole;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A parsed Sponge Schematic (v1, v2 or v3) - the format WorldEdit and FAWE
 * write. Holds the palette, the block index array, and any Spadefall markers
 * found among the structure-block entities.
 *
 * Only what the paster needs is read. Biomes and entities are ignored.
 */
public final class Schematic {

    private final int width, height, length;
    private final String[] palette;      // index -> block state string
    private final int[] blocks;          // x + z*width + y*width*length -> palette index
    private final List<Marker> markers;  // positions relative to the schematic origin
    private final String name;

    private Schematic(int width, int height, int length, String[] palette, int[] blocks,
                      List<Marker> markers, String name) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.palette = palette;
        this.blocks = blocks;
        this.markers = markers;
        this.name = name;
    }

    public static Schematic read(InputStream in) throws IOException {
        Map<String, Object> root = Nbt.readGzipped(in);

        // v3 nests everything under "Schematic"; v1/v2 keep it at the root.
        Map<String, Object> schem = Nbt.getCompound(root, "Schematic");
        if (schem == null) schem = root;
        int version = Nbt.getInt(schem, "Version", 2);

        int width = Nbt.getInt(schem, "Width", 0) & 0xFFFF;
        int height = Nbt.getInt(schem, "Height", 0) & 0xFFFF;
        int length = Nbt.getInt(schem, "Length", 0) & 0xFFFF;
        if (width == 0 || height == 0 || length == 0) {
            throw new IOException("Schematic has no size");
        }

        Map<String, Object> paletteMap;
        byte[] data;
        List<Object> blockEntities;
        if (version >= 3) {
            Map<String, Object> blocksSection = Nbt.getCompound(schem, "Blocks");
            if (blocksSection == null) throw new IOException("v3 schematic has no Blocks section");
            paletteMap = Nbt.getCompound(blocksSection, "Palette");
            data = (byte[]) blocksSection.get("Data");
            blockEntities = Nbt.getList(blocksSection, "BlockEntities");
        } else {
            paletteMap = Nbt.getCompound(schem, "Palette");
            data = (byte[]) schem.get("BlockData");
            blockEntities = Nbt.getList(schem, "BlockEntities");
            if (blockEntities == null) blockEntities = Nbt.getList(schem, "TileEntities");
        }
        if (paletteMap == null || data == null) {
            throw new IOException("Schematic is missing its palette or block data");
        }

        int max = 0;
        for (Object v : paletteMap.values()) {
            if (v instanceof Number n) max = Math.max(max, n.intValue());
        }
        String[] palette = new String[max + 1];
        for (Map.Entry<String, Object> e : paletteMap.entrySet()) {
            if (e.getValue() instanceof Number n) palette[n.intValue()] = e.getKey();
        }

        int total = width * height * length;
        int[] blocks = new int[total];
        int pos = 0;
        for (int i = 0; i < total; i++) {
            int value = 0, shift = 0;
            while (true) {
                if (pos >= data.length) throw new IOException("Block data ended early");
                int b = data[pos++] & 0xFF;
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) break;
                shift += 7;
            }
            blocks[i] = value;
        }

        List<Marker> markers = new ArrayList<>();
        if (blockEntities != null) {
            for (Object o : blockEntities) {
                if (!(o instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> be = (Map<String, Object>) o;
                String id = Nbt.getString(be, "Id");
                if (id == null || !id.endsWith("structure_block")) continue;
                int[] p = be.get("Pos") instanceof int[] arr ? arr : null;
                if (p == null || p.length < 3) continue;
                // v2 flattens the NBT into the entry; v3 puts it under "Data".
                String structureName = Nbt.getString(be, "name");
                if (structureName == null) {
                    Map<String, Object> inner = Nbt.getCompound(be, "Data");
                    if (inner != null) structureName = Nbt.getString(inner, "name");
                }
                MarkerRole.ParsedName parsed = MarkerRole.parse(structureName);
                if (parsed == null) continue;
                markers.add(new Marker(parsed.role(), p[0], p[1], p[2], parsed.value()));
            }
        }

        String name = null;
        Map<String, Object> meta = Nbt.getCompound(schem, "Metadata");
        if (meta != null) name = Nbt.getString(meta, "Name");

        return new Schematic(width, height, length, palette, blocks, markers, name);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getLength() { return length; }
    public String[] getPalette() { return palette; }
    public int[] getBlocks() { return blocks; }
    public List<Marker> getMarkers() { return markers; }
    public String getName() { return name; }
    public long getVolume() { return (long) width * height * length; }

    public int indexOf(int x, int y, int z) {
        return x + z * width + y * width * length;
    }
}
