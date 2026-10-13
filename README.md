# ♠ Spadefall

A descent race minigame for Paper. Start at the top, get to the bottom first.
Grab the spades on the way down if you think you can afford the detour — they
buy you a seat at the card table afterwards.

Free and open source, GPL-3.0. Built for and by [SuperCraft-MC](https://supercraft-mc.com).

---

## Status: 0.5.0 — playable, with a map in the box

Install the jar, run `/sf setup demo`, play. That's the whole setup.

What works today:

- **Playing**: join, countdown, scatter to the starts, fall, collect chips,
  cross the finish line, placements, standings, round winner
- **The bundled map**: Mount St. Helens — a crater you descend into a lava
  lake. Pasted by the plugin itself, no WorldEdit needed
- **Any schematic**: `/sf map load <file>` pastes a Sponge `.schem` and reads
  the markers out of it. See `docs/MAP-BUILDERS-GUIDE.md`
- Multi-map rounds, spectating, inventory save/restore, protections, capped
  fall damage, per-arena settings, scoreboard, stats
- In-game setup GUI, chat wizard, `/sf next`, `/sf missing`, marker glow

Still to come: spades and the Spade Table, the shops and perks, deathmatch.

---

## Requirements

- Paper **26.2+**
- Java **25** (required by Minecraft 26.1 and later)

No hard dependencies. PlaceholderAPI, Vault and FastAsyncWorldEdit are all
optional and capability-checked at runtime.

---

## Fastest start

```
/sf setup demo
```

Stand somewhere with room, confirm, and in a couple of seconds there's a
crater under you with an arena named `demo` wrapped around it. `/sf join demo`.

It's named *demo* on purpose — it's a generated map, good for seeing how the
game plays, not what you'd sell tickets to.

## Loading a map

Drop a `.schem` in `plugins/Spadefall/maps/` and:

```
/sf map load <file> [name]
```

It pastes with its base at your feet, reads any Spadefall markers baked in,
validates, and registers. Maps without markers paste fine; mark them up with
the tool afterwards.

## Setting up an arena

Join as an op on a server with no arenas and you'll be offered a click to
start. Or:

```
/sf setup
```

That opens the hub: pick an arena, or **Create new arena** and type a name in
chat. You're now in **edit mode** — the setup inventory opens, and every marker
on the map starts glowing in its colour with a label floating over it.

Each step is an item. Its colour is its status:

| Colour | Meaning |
|---|---|
| green | done |
| yellow | done, but with a warning you can override |
| red | missing or broken — the arena can't run yet |
| grey | optional, not set |

Hover to see what's needed. Click to do it: the Lobby item sets the lobby
where you stand, the marker items hand you the tool already set to that role,
and **Register** unlocks once nothing is red.

Two commands if you'd rather not open the GUI:

```
/sf next        the one thing to do next, with clickable buttons
/sf missing     the whole checklist
```

### Glow

| Marker | Colour |
|---|---|
| Player Start | green |
| Spade | dark purple |
| Chip | gold |
| Finish | red |
| Deathmatch Spawn | orange |
| Chest | cyan |
| Region edges | white |

Only you see it, only while editing. `/sf glow` toggles it.

### The tool

Golden shovel. Right-click places, left-click tells you what's there,
shift + left-click removes, shift + right-click or shift + scroll cycles the
role. Its name shows the current role, and your action bar shows how many
you've placed.

Chip denominations follow poker: 1 white, 5 red, 25 green, 100 black, 500
purple. Set one with `/sf tool value 25` before placing.

### Region

Auto by default: the bounding box of your markers, padded. You'll see it as
white glowing edges. Shift-click the Region item for a manual wand if auto
doesn't suit the build.

### Maps that describe themselves

Markers can also be real **structure blocks** with the role in the name
field — `spadefall:spawn`, `spadefall:chip:25` and so on. `/sf tool mode stamp`
writes them into the world as you place, so a map can be exported and shared
with its markers baked in. **Import stamped markers** in the GUI reads them
back out of a downloaded map.

| Structure block name | Meaning |
|---|---|
| `spadefall:spawn` | one player start slot |
| `spadefall:finish` | the goal |
| `spadefall:spade` | a *candidate* spade location |
| `spadefall:chip:25` | a *candidate* chip, with denomination |
| `spadefall:dm_spawn` | deathmatch start slot (final map only) |
| `spadefall:chest` | optional loot point |

Spade and chip markers are a **pool** — mark forty spots and each round draws
a handful, so the map plays differently every time.

---

## Capacity resolves itself

The map's own **spawn marker count** is its capacity. The effective cap is
`min(map capacity, config max-players)` — the smaller number always wins, so a
12-spawn community map on a 64-slot server just runs 12, and a 64-spawn map on
a 4GB box runs whatever you configured. You never reconcile the two by hand.

---

## Validation

`/sf doctor` checks every map and arena and reports problems. The distinction
matters:

- **Errors** are structural. The map cannot run and is refused.
- **Warnings** are judgement calls, and you can always override them.

The classic warning is *"64 spawn markers but only 3 spade candidates"* —
perfectly playable, almost certainly not what you meant. You'll be asked to
confirm, and `/sf confirm` proceeds anyway.

Startup logs the same warnings and never blocks.

---

## Playing

```
/sf join [arena]        join; the fullest open arena if none named
/sf leave               back to where you were, with your own inventory
```

The countdown starts at `min-players` (config, or per arena). When it hits
zero everyone's scattered across the start markers, held for three seconds,
then released. Get to the finish line - a Y level across the whole map - first.

Chips spin on the way down - poker chips, coloured by value: white 1, red 5,
green 25, blue 100, black 500. Walk through one to take it. Chips are your
wallet; score comes from placement, with a small flat amount per chip, so the
fast player wins the round and the patient one gets rich. First down also
pays a chip bonus.

Players who finish spectate from above the line until everyone's down or the
timer runs out. Nobody's eliminated - run out of time and you take last-place
points and come along to the next map.

Fall damage is **on** by default - a long drop kills and respawns you at your
start, which is what makes the ledges matter. `game.fall-damage-multiplier`
turns it up. You finish by *landing* at or below the line and standing there
alive and undamaged for two seconds; a dive that kills you on impact never
counts. `game.fall-damage: CAPPED` for a casual server.

Deathmatch is off by default - the descent is the game. `/sf arena set <arena>
deathmatch true` turns it on per arena once that phase lands.

## Commands

| Command | Permission |
|---|---|
| `/sf join [arena]` | `spadefall.play` |
| `/sf leave` | `spadefall.play` |
| `/sf start [arena]` | `spadefall.admin` |
| `/sf stop [arena]` | `spadefall.admin` |
| `/sf setup [arena\|chat\|done]` | `spadefall.admin` |
| `/sf next` / `/sf missing` | `spadefall.admin` |
| `/sf glow` | `spadefall.admin` |
| `/sf tool [role\|value\|mode\|status\|wand]` | `spadefall.admin` |
| `/sf arena create\|delete\|list\|setlobby\|maps\|set` | `spadefall.admin` |
| `/sf map pos1\|pos2\|scan\|list\|info\|delete` | `spadefall.admin` |
| `/sf doctor [map]` | `spadefall.admin` |
| `/sf confirm` / `/sf cancel` | `spadefall.admin` |
| `/sf reload` | `spadefall.admin` |

Per-arena settings: `/sf arena set <arena> minplayers|autostart|countdown <value|default>`,
or click Settings in the setup GUI.

---

## Building

You don't need to. Push to GitHub and Actions builds it, attaching the jar to
the workflow run. Tag a release (`v1.0.0`) and it attaches the jar to the
release too.

Locally, if you want:

```
gradle build
```

Output lands in `build/libs/`.

### If the first CI build fails

Check `gradle.properties`. Mojang moved to year-based versions, so Paper's
artifact version tracks the game version:

```properties
paperApiVersion=26.2-R0.1-SNAPSHOT
```

If Gradle reports *"Could not find io.papermc.paper:paper-api"*, that one line
is almost certainly the cause. The correct value is listed at
<https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/>.
Nothing else needs to change.

---

## License

GPL-3.0. Fork it, improve it, sell it if you like — but any fork stays open.
That's the point.
