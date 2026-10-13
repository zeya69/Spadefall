#!/usr/bin/env python3
"""
Spadefall demo map generator: Mount St. Helens.

A crater you descend the *inside* of. Start on the glacier rim, drop through
terraced ash and basalt, cross the finish line into the lava lake. The north
face is blown out - the breach - a faster, more exposed way in.

Output is a Sponge Schematic v2 (.schem) with Spadefall markers baked in as
structure-block entities, so the plugin can paste it and set itself up.

    python3 tools/gen_sthelens.py [seed] [out.schem]
"""
import gzip
import io
import math
import random
import sys

import nbtlib
from nbtlib import tag

# ---------------------------------------------------------------------------
# dimensions
# ---------------------------------------------------------------------------
W = L = 71            # footprint
H = 112               # height
CX = CZ = W // 2      # centre column

LAKE_Y = 6            # lava surface sits at LAKE_Y (block at LAKE_Y is lava)
FINISH_Y = 8          # finish marker height -> line drawn at y=8, crossing at 8.5
RIM_Y = 104           # walkable rim height
DM_Y = 96             # lava dome plateau

R_OUT = 35.0          # outer radius of the mountain cylinder
R_RIM = 31.0          # crater radius at the rim
R_LAKE = 11.0         # crater radius at the lake

SEED = int(sys.argv[1]) if len(sys.argv) > 1 else 1980
OUT = sys.argv[2] if len(sys.argv) > 2 else "st-helens.schem"
rng = random.Random(SEED)

# ---------------------------------------------------------------------------
# palette (orange + blue, black and grey between)
# ---------------------------------------------------------------------------
AIR = "minecraft:air"
LAVA = "minecraft:lava"
WATER = "minecraft:water"
MAGMA = "minecraft:magma_block"
BASALT = "minecraft:basalt[axis=y]"
POL_BASALT = "minecraft:polished_basalt[axis=y]"
SMOOTH_BASALT = "minecraft:smooth_basalt"
BLACKSTONE = "minecraft:blackstone"
DEEPSLATE = "minecraft:deepslate[axis=y]"
TUFF = "minecraft:tuff"
STONE = "minecraft:stone"
ANDESITE = "minecraft:andesite"
GRAVEL_LOOK = "minecraft:cobbled_deepslate"
ASH = "minecraft:gray_concrete"
LIGHT_ASH = "minecraft:light_gray_concrete"
BLACK_C = "minecraft:black_concrete"
OBSIDIAN = "minecraft:obsidian"
CRYING = "minecraft:crying_obsidian"
SNOW = "minecraft:snow_block"
PACKED_ICE = "minecraft:packed_ice"
BLUE_ICE = "minecraft:blue_ice"
ORANGE_C = "minecraft:orange_concrete"
ORANGE_T = "minecraft:orange_terracotta"
BLUE_C = "minecraft:blue_concrete"
SLIME = "minecraft:slime_block"
SHROOM = "minecraft:shroomlight"
GLOW = "minecraft:glowstone"
SEA_LANTERN = "minecraft:sea_lantern"
CAMPFIRE = "minecraft:soul_campfire[lit=true,facing=north,signal_fire=false,waterlogged=false]"
SPRUCE_LOG = "minecraft:spruce_log[axis=y]"
SPRUCE_LEAVES = "minecraft:spruce_leaves[distance=1,persistent=true,waterlogged=false]"
STRUCTURE = "minecraft:structure_block[mode=save]"

WALL_MIX = [BASALT, BASALT, BLACKSTONE, SMOOTH_BASALT, DEEPSLATE, TUFF, TUFF, BLACK_C]
UPPER_MIX = [STONE, ANDESITE, TUFF, LIGHT_ASH, ASH, STONE, GRAVEL_LOOK]
LEDGE_MIX = [TUFF, ASH, LIGHT_ASH, ANDESITE, TUFF]
RIM_MIX = [SNOW, PACKED_ICE, SNOW, BLUE_ICE, PACKED_ICE, STONE]

# ---------------------------------------------------------------------------
# voxel grid
# ---------------------------------------------------------------------------
grid = {}            # (x,y,z) -> block state string ; absent = air
markers = []         # (x,y,z, name)


def put(x, y, z, block):
    if 0 <= x < W and 0 <= y < H and 0 <= z < L:
        grid[(x, y, z)] = block


def get(x, y, z):
    return grid.get((x, y, z), AIR)


def clear(x, y, z):
    grid.pop((x, y, z), None)


def mark(x, y, z, name):
    markers.append((x, y, z, name))
    clear(x, y, z)   # the plugin records markers and never places the block


def dist(x, z):
    return math.hypot(x - CX, z - CZ)


def angle(x, z):
    return math.degrees(math.atan2(z - CZ, x - CX)) % 360.0


def in_breach(a, width=28.0):
    """North face (-z) is blown out. atan2 with z-CZ negative -> angle ~270."""
    d = abs(((a - 270.0) + 180.0) % 360.0 - 180.0)
    return d <= width


def crater_radius(y):
    """Inner radius: narrow at the lake, wide at the rim. Slight bulge mid-way."""
    t = max(0.0, min(1.0, (y - LAKE_Y) / (RIM_Y - LAKE_Y)))
    base = R_LAKE + (R_RIM - R_LAKE) * (t ** 1.25)
    bulge = 1.5 * math.sin(t * math.pi)
    return base + bulge


# ---------------------------------------------------------------------------
# 1. the mountain
# ---------------------------------------------------------------------------
for y in range(0, RIM_Y + 1):
    r_in = crater_radius(y)
    for x in range(W):
        for z in range(L):
            d = dist(x, z)
            if d > R_OUT:
                continue
            a = angle(x, z)
            # breach: the north wall is cut down to a slope
            breach_cut = 0
            if in_breach(a):
                depth = (28.0 - abs(((a - 270.0) + 180.0) % 360.0 - 180.0)) / 28.0  # 1 at centre
                breach_cut = int(34 * depth)
            top = RIM_Y - breach_cut
            if y > top:
                continue
            if d < r_in:
                continue  # inside the crater: air
            # wall vs interior noise
            if y < 30:
                block = rng.choice(WALL_MIX)
            elif y < 75:
                block = rng.choice(WALL_MIX if d - r_in < 3 else UPPER_MIX)
            else:
                block = rng.choice(UPPER_MIX if d - r_in < 4 else [STONE, TUFF, ANDESITE])
            # glacier crown on the upper inner walls
            if y > RIM_Y - 14 and d - r_in < 2.5 and not in_breach(a, 36):
                block = rng.choice([PACKED_ICE, BLUE_ICE, PACKED_ICE, SNOW])
            put(x, y, z, block)

# surface dressing on the rim and breach slope
for x in range(W):
    for z in range(L):
        d = dist(x, z)
        if d > R_OUT:
            continue
        # find top
        top = None
        for y in range(RIM_Y, -1, -1):
            if (x, y, z) in grid:
                top = y
                break
        if top is None:
            continue
        a = angle(x, z)
        if top >= RIM_Y - 1 and not in_breach(a, 36):
            put(x, top, z, rng.choice(RIM_MIX))
            if rng.random() < 0.18 and d < R_OUT - 1 and d > crater_radius(top) + 3:
                put(x, top + 1, z, "minecraft:snow[layers=%d]" % rng.randint(1, 3))
        elif in_breach(a, 36):
            put(x, top, z, rng.choice([ASH, LIGHT_ASH, TUFF, ASH]))

# sparse spruce on the outer rim
for _ in range(26):
    a = rng.uniform(0, 360)
    if in_breach(a, 40):
        continue
    d = rng.uniform(R_RIM + 1.5, R_OUT - 0.5)
    x = int(CX + d * math.cos(math.radians(a)))
    z = int(CZ + d * math.sin(math.radians(a)))
    top = None
    for y in range(RIM_Y + 2, 0, -1):
        if (x, y, z) in grid:
            top = y
            break
    if top is None:
        continue
    h = rng.randint(4, 7)
    for i in range(1, h + 1):
        put(x, top + i, z, SPRUCE_LOG)
    for dy in range(h - 2, h + 2):
        rad = 2 if dy < h else 1
        for dx in range(-rad, rad + 1):
            for dz in range(-rad, rad + 1):
                if abs(dx) + abs(dz) <= rad + 1 and (dx or dz or dy >= h):
                    if get(x + dx, top + dy, z + dz) == AIR:
                        put(x + dx, top + dy, z + dz, SPRUCE_LEAVES)

# ---------------------------------------------------------------------------
# 2. the lava lake and finish
# ---------------------------------------------------------------------------
for x in range(W):
    for z in range(L):
        d = dist(x, z)
        if d <= R_LAKE + 0.5:
            for y in range(0, LAKE_Y + 1):
                put(x, y, z, LAVA if y >= 3 else OBSIDIAN)
        elif d <= R_LAKE + 3.5:
            # basalt shore, slightly raised, with magma glow
            for y in range(0, LAKE_Y + 2):
                put(x, y, z, MAGMA if (y == LAKE_Y + 1 and rng.random() < 0.3) else POL_BASALT)
# a glass lid over the lake: you land ON the glow, you don't go in. The finish
# is the caldera floor - shore and lid both sit at LAKE_Y+1, feet at LAKE_Y+2.
GLASS = "minecraft:glass"
for x in range(W):
    for z in range(L):
        if dist(x, z) <= R_LAKE + 0.5:
            put(x, LAKE_Y + 1, z, GLASS)
# finish marker: one is enough, Y is what matters. Put it on the shore so it's visible in glow.
mark(CX + int(R_LAKE) + 2, FINISH_Y, CZ, "spadefall:finish")
mark(CX - int(R_LAKE) - 2, FINISH_Y, CZ, "spadefall:finish")

# ---------------------------------------------------------------------------
# 3. terraces - the actual descent
# ---------------------------------------------------------------------------
chip_spots = []     # (x,y,z, difficulty 0..1)
spade_spots = []
chest_spots = []

level_ys = list(range(LAKE_Y + 10, RIM_Y - 8, 8))
prev_arcs = []
for li, y in enumerate(level_ys):
    r_in = crater_radius(y)
    n_arcs = rng.randint(3, 5)
    arcs = []
    tries = 0
    while len(arcs) < n_arcs and tries < 40:
        tries += 1
        start = rng.uniform(0, 360)
        span = rng.uniform(35, 85)
        # don't stack directly under the previous level's arcs: force offset
        clash = any(abs(((start - ps) + 180) % 360 - 180) < 25 for ps, _ in prev_arcs)
        if clash and rng.random() < 0.75:
            continue
        arcs.append((start, span))
    prev_arcs = arcs

    for (start, span) in arcs:
        depth = rng.randint(3, 6)
        hard = 0.0
        if in_breach((start + span / 2) % 360, 40):
            depth = rng.randint(2, 4)   # breach ledges are narrow
            hard = 0.6
        feature = rng.choice(["plain", "plain", "water", "slime", "lavapot", "plain"])
        ledge_cells = []   # (x, z, k) where k = 0 at the wall, depth at the tip
        for da in range(0, int(span) + 1):
            a = (start + da) % 360
            for k in range(0, depth + 1):
                d = r_in + 0.5 - k            # step INWARD from the wall, out over the drop
                x = int(round(CX + d * math.cos(math.radians(a))))
                z = int(round(CZ + d * math.sin(math.radians(a))))
                if (x, y, z) not in grid:
                    put(x, y, z, rng.choice(LEDGE_MIX))
                    ledge_cells.append((x, z, k))
                    # thicken the ledge near the wall so it reads as rock, not paper
                    if k < 2 and rng.random() < 0.6:
                        put(x, y - 1, z, rng.choice(WALL_MIX))
        if not ledge_cells:
            continue

        # feature in the middle of the ledge
        mid_a = (start + span / 2) % 360
        mx = int(round(CX + (r_in - depth / 2) * math.cos(math.radians(mid_a))))
        mz = int(round(CZ + (r_in - depth / 2) * math.sin(math.radians(mid_a))))
        if feature == "water" and depth >= 4:
            for dx in range(-1, 2):
                for dz in range(-1, 2):
                    if (mx + dx, y, mz + dz) in grid:
                        put(mx + dx, y, mz + dz, WATER)
            for dx in range(-2, 3):
                for dz in range(-2, 3):
                    if max(abs(dx), abs(dz)) == 2 and (mx + dx, y, mz + dz) in grid:
                        put(mx + dx, y, mz + dz, BLUE_C)
        elif feature == "slime":
            put(mx, y, mz, SLIME)
            put(mx + 1, y, mz, SLIME)
        elif feature == "lavapot" and depth >= 4:
            put(mx, y, mz, LAVA)
            for dx in range(-1, 2):
                for dz in range(-1, 2):
                    if (dx or dz) and (mx + dx, y, mz + dz) in grid:
                        put(mx + dx, y, mz + dz, rng.choice([MAGMA, OBSIDIAN, BLACKSTONE]))
            hard += 0.2

        # lighting: a campfire or shroomlight tucked against the wall
        wx = int(round(CX + (r_in + 0.6) * math.cos(math.radians(mid_a))))
        wz = int(round(CZ + (r_in + 0.6) * math.sin(math.radians(mid_a))))
        if rng.random() < 0.5:
            if get(wx, y, wz) != AIR:
                put(wx, y + 1, wz, CAMPFIRE)
        else:
            put(wx, y + 2, wz, SHROOM)

        # chips: the tip of a ledge (furthest out over the drop) is the hard spot
        tip_cells = [c for c in ledge_cells if c[2] >= depth - 1]
        inner_cells = [c for c in ledge_cells if c[2] <= 1]
        for _ in range(rng.randint(1, 3)):
            if inner_cells and rng.random() < 0.6:
                x, z, _k = rng.choice(inner_cells)
                chip_spots.append((x, y + 1, z, min(1.0, hard + 0.1)))
            elif tip_cells:
                x, z, _k = rng.choice(tip_cells)
                chip_spots.append((x, y + 1, z, min(1.0, hard + 0.5)))
        if tip_cells and rng.random() < 0.45:
            x, z, _k = rng.choice(tip_cells)
            spade_spots.append((x, y + 1, z))
        if inner_cells and rng.random() < 0.25:
            x, z, _k = rng.choice(inner_cells)
            chest_spots.append((x, y + 1, z))

# ---- survivability: a water landing under the breach, and no sector with a
# ---- gap of more than MAX_GAP blocks between consecutive ledges
MAX_GAP = 20

def ledge_arc(y, start, span, depth, feature=None):
    r_in = crater_radius(y)
    cells = []
    for da in range(0, int(span) + 1):
        a = (start + da) % 360
        for k in range(0, depth + 1):
            d = r_in + 0.5 - k
            x = int(round(CX + d * math.cos(math.radians(a))))
            z = int(round(CZ + d * math.sin(math.radians(a))))
            if (x, y, z) not in grid:
                put(x, y, z, rng.choice(LEDGE_MIX))
                cells.append((x, z, k))
                if k < 2:
                    put(x, y - 1, z, rng.choice(WALL_MIX))
    if feature == "water" and cells:
        mid_a = (start + span / 2) % 360
        mx = int(round(CX + (r_in - depth / 2) * math.cos(math.radians(mid_a))))
        mz = int(round(CZ + (r_in - depth / 2) * math.sin(math.radians(mid_a))))
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                if (mx + dx, y, mz + dz) in grid:
                    put(mx + dx, y, mz + dz, WATER)
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if max(abs(dx), abs(dz)) == 2 and (mx + dx, y, mz + dz) in grid:
                    put(mx + dx, y, mz + dz, BLUE_C)
    return cells

# the breach drops from ~y70 at its centre; catch it at 56 with water
ledge_arc(56, 270 - 26, 52, 6, feature="water")

# gap check: 12 sectors of 30 degrees. For each, find the ledge levels that
# have blocks in that sector; where two consecutive ones are > MAX_GAP apart
# (or the rim/lid is), drop in a small catch ledge at the midpoint.
def sector_levels(sector):
    a0, a1 = sector * 30, sector * 30 + 30
    levels = []
    for y in level_ys + [56]:
        r_in = crater_radius(y)
        hit = False
        for a in range(a0, a1, 3):
            for k in range(1, 4):
                d = r_in + 0.5 - k
                x = int(round(CX + d * math.cos(math.radians(a))))
                z = int(round(CZ + d * math.sin(math.radians(a))))
                if (x, y, z) in grid:
                    hit = True
                    break
            if hit:
                break
        if hit:
            levels.append(y)
    return sorted(set(levels))

added = 0
for sector in range(12):
    centre = sector * 30 + 15
    top = RIM_Y - (int(34 * (28.0 - abs(((centre - 270.0) + 180.0) % 360.0 - 180.0)) / 28.0) if in_breach(centre) else 0)
    floor = LAKE_Y + 2
    pts = [floor] + sector_levels(sector) + [top]
    for i in range(len(pts) - 1):
        lo, hi = pts[i], pts[i + 1]
        while hi - lo > MAX_GAP:
            mid = lo + (hi - lo) // 2
            ledge_arc(mid, sector * 30 + 4, 22, 3, feature="water" if rng.random() < 0.5 else None)
            added += 1
            lo = mid
print("catch ledges added by gap check:", added)

# magma seams down the walls (two streaks)
for seam_a in (rng.uniform(0, 360), rng.uniform(0, 360)):
    for y in range(LAKE_Y + 4, RIM_Y - 20):
        a = seam_a + math.sin(y / 9.0) * 6
        r_in = crater_radius(y)
        x = int(round(CX + (r_in + 0.4) * math.cos(math.radians(a))))
        z = int(round(CZ + (r_in + 0.4) * math.sin(math.radians(a))))
        if (x, y, z) in grid:
            put(x, y, z, MAGMA if rng.random() < 0.7 else CRYING)

# glowstone pockets in the lower walls for visibility
for _ in range(60):
    y = rng.randint(LAKE_Y + 3, 60)
    a = rng.uniform(0, 360)
    r_in = crater_radius(y)
    x = int(round(CX + (r_in + 0.5) * math.cos(math.radians(a))))
    z = int(round(CZ + (r_in + 0.5) * math.sin(math.radians(a))))
    if (x, y, z) in grid:
        put(x, y, z, GLOW if rng.random() < 0.5 else SEA_LANTERN)

# ---------------------------------------------------------------------------
# 4. starts on the rim (24), skipping the breach
# ---------------------------------------------------------------------------
starts = 0
for i in range(32):
    a = i * (360.0 / 32)
    if in_breach(a, 34):
        continue
    if starts >= 24:
        break
    d = R_RIM + 1.5
    x = int(round(CX + d * math.cos(math.radians(a))))
    z = int(round(CZ + d * math.sin(math.radians(a))))
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            put(x + dx, RIM_Y, z + dz, PACKED_ICE if (dx or dz) else BLUE_ICE)
            clear(x + dx, RIM_Y + 1, z + dz)
            clear(x + dx, RIM_Y + 2, z + dz)
    mark(x, RIM_Y + 1, z, "spadefall:spawn")
    starts += 1

# breach-edge purples: the risky prize
for _ in range(4):
    a = 270 + rng.uniform(-18, 18)
    y = RIM_Y - 34 + rng.randint(0, 6)
    d = crater_radius(y) + rng.uniform(0.5, 2.5)
    x = int(round(CX + d * math.cos(math.radians(a))))
    z = int(round(CZ + d * math.sin(math.radians(a))))
    top = None
    for yy in range(y + 6, y - 6, -1):
        if (x, yy, z) in grid:
            top = yy
            break
    if top is not None:
        chip_spots.append((x, top + 1, z, 1.0))

# ---------------------------------------------------------------------------
# 5. the lava dome - deathmatch plateau in the south-east corner
# ---------------------------------------------------------------------------
dcx, dcz, dr = W - 12, L - 12, 10
for x in range(W):
    for z in range(L):
        d = math.hypot(x - dcx, z - dcz)
        if d <= dr:
            h = int(6 * (1 - (d / dr) ** 2))
            for y in range(DM_Y - 8, DM_Y + 1):
                put(x, y, z, rng.choice([BASALT, SMOOTH_BASALT, BLACKSTONE]))
            put(x, DM_Y, z, rng.choice([ORANGE_T, ORANGE_C, MAGMA] if d < 3 else [BASALT, TUFF, BLACKSTONE]))
            for y in range(DM_Y + 1, DM_Y + 3):
                clear(x, y, z)
for i in range(8):
    a = i * 45
    x = int(round(dcx + (dr - 2) * math.cos(math.radians(a))))
    z = int(round(dcz + (dr - 2) * math.sin(math.radians(a))))
    mark(x, DM_Y + 1, z, "spadefall:dm_spawn")
# a bridge from the rim to the dome so it's reachable on foot
for t in range(0, 40):
    x = int(round(CX + (R_RIM + 2) * math.cos(math.radians(45)) + t * (dcx - (CX + (R_RIM + 2) * math.cos(math.radians(45)))) / 40))
    z = int(round(CZ + (R_RIM + 2) * math.sin(math.radians(45)) + t * (dcz - (CZ + (R_RIM + 2) * math.sin(math.radians(45)))) / 40))
    y = int(round(RIM_Y + (DM_Y - RIM_Y) * t / 40))
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            put(x + dx, y, z + dz, POL_BASALT)
            clear(x + dx, y + 1, z + dz)
            clear(x + dx, y + 2, z + dz)

# ---------------------------------------------------------------------------
# 6. chips, spades, chests -> markers
# ---------------------------------------------------------------------------
def denomination(h):
    """Harder spot -> bigger chip. Purples are rare and earned."""
    roll = rng.random() * 0.9 + h * 0.75
    if roll > 1.45:
        return 500
    if roll > 1.12:
        return 100
    if roll > 0.78:
        return 25
    if roll > 0.42:
        return 5
    return 1


seen = set()
for (x, y, z, h) in chip_spots:
    if (x, y, z) in seen or get(x, y, z) != AIR:
        continue
    seen.add((x, y, z))
    mark(x, y, z, "spadefall:chip:%d" % denomination(h))
for (x, y, z) in spade_spots:
    if (x, y, z) in seen or get(x, y, z) != AIR:
        continue
    seen.add((x, y, z))
    mark(x, y, z, "spadefall:spade")
for (x, y, z) in chest_spots:
    if (x, y, z) in seen or get(x, y, z) != AIR:
        continue
    seen.add((x, y, z))
    mark(x, y, z, "spadefall:chest")

# ---------------------------------------------------------------------------
# 7. write Sponge Schematic v2
# ---------------------------------------------------------------------------
palette = {AIR: 0}
data = bytearray()
for y in range(H):
    for z in range(L):
        for x in range(W):
            b = grid.get((x, y, z), AIR)
            idx = palette.get(b)
            if idx is None:
                idx = len(palette)
                palette[b] = idx
            # varint
            v = idx
            while True:
                byte = v & 0x7F
                v >>= 7
                if v:
                    data.append(byte | 0x80)
                else:
                    data.append(byte)
                    break

block_entities = tag.List[tag.Compound]()
for (x, y, z, name) in markers:
    block_entities.append(tag.Compound({
        "Pos": tag.IntArray([x, y, z]),
        "Id": tag.String("minecraft:structure_block"),
        "name": tag.String(name),
        "mode": tag.String("SAVE"),
    }))
# the marker positions must exist in the palette as structure blocks so a
# WorldEdit paste places them; our own paster records them and skips the block.
if STRUCTURE not in palette:
    palette[STRUCTURE] = len(palette)
sidx = palette[STRUCTURE]
# rewrite those positions in data: simpler to rebuild data with markers inserted
marker_pos = {(x, y, z) for (x, y, z, _n) in markers}
data = bytearray()
for y in range(H):
    for z in range(L):
        for x in range(W):
            if (x, y, z) in marker_pos:
                idx = sidx
            else:
                idx = palette[grid.get((x, y, z), AIR)]
            v = idx
            while True:
                byte = v & 0x7F
                v >>= 7
                if v:
                    data.append(byte | 0x80)
                else:
                    data.append(byte)
                    break

root = tag.Compound({
    "Version": tag.Int(2),
    "DataVersion": tag.Int(4189),
    "Width": tag.Short(W),
    "Height": tag.Short(H),
    "Length": tag.Short(L),
    "Offset": tag.IntArray([0, 0, 0]),
    "PaletteMax": tag.Int(len(palette)),
    "Palette": tag.Compound({k: tag.Int(v) for k, v in palette.items()}),
    "BlockData": tag.ByteArray([b - 256 if b > 127 else b for b in data]),
    "BlockEntities": block_entities,
    "Metadata": tag.Compound({
        "Name": tag.String("Mount St. Helens"),
        "Author": tag.String("Spadefall / SuperCraft-MC"),
        "SpadefallSeed": tag.Int(SEED),
    }),
})
f = nbtlib.File(root, gzipped=True, root_name="Schematic")
f.save(OUT)

# ---------------------------------------------------------------------------
# report
# ---------------------------------------------------------------------------
from collections import Counter
roles = Counter(n.split(":")[1] for (_x, _y, _z, n) in markers)
chips = Counter(int(n.split(":")[2]) for (_x, _y, _z, n) in markers if n.startswith("spadefall:chip:"))
print("wrote", OUT)
print("size %dx%dx%d, %d non-air blocks, palette %d" % (W, H, L, len(grid), len(palette)))
print("markers:", dict(roles))
print("chip denominations:", dict(sorted(chips.items())))
print("finish Y =", FINISH_Y, " rim Y =", RIM_Y, " drop =", RIM_Y + 1 - FINISH_Y)
