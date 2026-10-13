# Spadefall — Map Builder's Guide

What a Spadefall map needs, how to mark it up, and how to hand it over so it
sets itself up on any server. Read this before building; it's short.

---

## The game, in one paragraph

Players start at the top. They have to get to the **finish line** — a Y level,
not a spot — first. On the way down there are **chips** to collect (score and
currency) and **spades** sitting off the fast route that buy a seat at the
card table afterwards. Nobody is eliminated for being slow. After the last
map there's a deathmatch on a separate flat area.

So a map is: a tall descent, with choices.

---

## Shape

- **Tall.** 80–120 blocks of drop is the sweet spot. Under 30 and the plugin
  warns you; it's barely a fall.
- **Several ways down.** The whole game dies if there's one obvious line.
  Aim for three or four routes with different risk: a safe slow one, a fast
  exposed one, and something in between.
- **Ledges, not stairs.** Players should be *falling* between platforms, not
  walking. Gaps of 6–12 blocks between landing spots feel right. Water, slime
  and hay all break falls — use them to make the risky routes survivable.
- **Everyone can see everyone.** Open shapes — a crater, a shaft, a canyon —
  beat enclosed ones. Watching the player ahead of you take a shortcut is half
  the fun.
- **Fits in 256 × 384 × 256.** That's the default size cap. The bundled
  demo is 71 × 112 × 71 and plays well.

---

## Markers

A marker is a **structure block** with a role typed into its name field. The
plugin reads them and removes the block when it pastes, so they never appear
in the finished map.

| Name field | Place at | How many |
|---|---|---|
| `spadefall:spawn` | one player's start position (feet) | one per player slot — the count IS the capacity |
| `spadefall:finish` | anywhere at finish height | 1 is enough; lowest one sets the line |
| `spadefall:spade` | a *candidate* spade spot, off the fast line | 6+ for a 12-player map; more is better |
| `spadefall:chip:1` … `chip:500` | a *candidate* chip spot | 40+; they're a pool, each round draws some |
| `spadefall:dm_spawn` | deathmatch start | 8+, on a flat area away from the finish |
| `spadefall:chest` | optional loot | whatever |

Place the structure block **where the player or item should be** — the air
block they stand in, not the floor under it.

### The finish is a line

One `spadefall:finish` block sets the finish **Y**. Any player who drops to
that height anywhere inside the map has finished. You don't need a landing
pad with markers on it; you need a height. The demo puts the line one block
above a lava lake so you fall *into* the glow and get pulled out.

### Chip denominations

Follow poker chips. The colour tells players what they're reaching for.

| Value | Colour | Where |
|---|---|---|
| 1 | white | on the obvious route |
| 5 | red | on the obvious route |
| 25 | green | a step off it |
| 100 | black | out on a ledge that costs time |
| 500 | purple | the risky spot nobody takes without thinking |

The map's difficulty curve is authored by *where you put the purples*. Don't
scatter them evenly. Put them on the breach edge, the far ledge, the one
spot that makes a player in the lead hesitate.

### Spades

Visible from the main line, not on it. A spade you have to memorise is a
secret; a spade you can see is a decision. Mark more candidates than you'd
want in one round — the plugin picks a few at random each time, so the map
plays differently.

---

## Hazards

- **Fall damage is on** by default. A 23-block fall onto stone kills; the
  player respawns at their start with a short freeze. That's the point: the
  straight dive has to lose, or nobody touches a ledge. Keep consecutive
  landings within about 15 blocks on every route, and use water or slime for
  anything longer.
- **Finishing means landing.** A player is down once they've stood on the
  ground at or below the line, alive, for a second. Falling across the line
  mid-air counts for nothing. Put something solid at the finish - the demo
  glasses over its lava lake so you land *on* the glow.
- **Lava** anywhere except the finish lake sends the player back to their
  start with a short freeze. Use it as a route-shaper. Contain it — a 1-deep
  basin with solid blocks around — so it doesn't flow onto your ledges.
- **Falling out** of the map's box does the same thing.
- **Water and slime** are landing pads. A 3×3 pool, one deep, fully
  enclosed, is enough.

---

## Light it

Players are falling fast in a tall space. Glowstone, shroomlight, sea
lanterns and campfires tucked into walls make ledges readable from above.
Dark maps play badly.

---

## Hand it over

Two ways, depending on how you built it.

**Built in-game on a Spadefall server:** `/sf setup`, mark everything up with
the tool, set the tool to **stamp** mode (`/sf tool mode stamp`) so real
structure blocks go into the world, then select the region with WorldEdit
and `//schem save <name>`. The markers travel with the schematic.

**Built elsewhere:** place structure blocks with the names above, save the
schematic. Any Sponge `.schem` (WorldEdit / FAWE) works, versions 1–3.

**To use it:** drop the file in `plugins/Spadefall/maps/` and run
`/sf map load <file>`. It pastes with its base at your feet, reads the
markers, validates, and registers. If anything's missing — a start below the
finish, no deathmatch spawns — it tells you, and `/sf setup` shows you where.

---

## Checklist before you ship

- [ ] At least 2 starts, all above the finish line
- [ ] A `finish` marker at the line's height
- [ ] 8+ `dm_spawn` on a flat area away from the finish
- [ ] 40+ chip candidates, purples where they hurt
- [ ] 6+ spade candidates, visible, off-route
- [ ] Three or more viable routes down, no drop over ~20 without water or slime
- [ ] Lit
- [ ] Fits 256 × 384 × 256
- [ ] Loads clean with `/sf map load` — no red in `/sf missing`

---

*Spadefall is free and open source. Maps are yours; sell them, share them,
submit them to supercraft-mc.com.*
