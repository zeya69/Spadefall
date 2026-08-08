# ♠ Spadefall

A descent race minigame for Paper. Start at the top, get to the bottom first.
Grab the spades on the way down if you think you can afford the detour — they
buy you a seat at the card table afterwards.

Free and open source, GPL-3.0. Built for and by [SuperCraft-MC](https://supercraft-mc.com).

---

## Status: slice 1 of 8

This is the foundation, not the game. What works today:

- Configuration, messages, SQLite/MySQL storage
- Arena registry and the round phase machine (waiting → countdown)
- **The map pipeline** — marker scanning, validation, and the marker tool

Descent, spades, deathmatch, the card table, shops and perks land in later
slices. The phase machine is deliberately exercisable now so the plumbing can
be tested before any gameplay exists.

---

## Requirements

- Paper **26.2+**
- Java **25** (required by Minecraft 26.1 and later)

No hard dependencies. PlaceholderAPI, Vault and FastAsyncWorldEdit are all
optional and capability-checked at runtime.

---

## Getting a map in

Spadefall maps describe themselves. Instead of setting fifty positions with
commands, you place **structure blocks** whose name field declares what they
are, and the plugin reads them.

| Structure block name | Meaning |
|---|---|
| `spadefall:spawn` | one player start slot |
| `spadefall:finish` | the goal |
| `spadefall:spade` | a *candidate* spade location |
| `spadefall:chip:25` | a *candidate* chip, with denomination |
| `spadefall:dm_spawn` | deathmatch start slot (final map only) |
| `spadefall:chest` | optional loot point |

Spade and chip markers are a **pool**, not fixed placements. Mark forty
possible spade spots and each round draws a handful at random — the map plays
differently every time, and you don't have to balance placement perfectly.

Chip denominations follow poker: 1 white, 5 red, 25 green, 100 black, 500
purple. Put whites on the main route and purples out on the nasty ledges. The
difficulty curve is authored by the builder, not configured by the owner.

### The marker tool

Downloaded a map that has no markers? Use the tool.

```
/sf tool                     # golden shovel
/sf tool role spade          # pick what you're placing
/sf tool value 100           # denomination, for chips
/sf tool mode stamp          # see below
```

Right-click places, left-click removes, shift-right-click cycles the role.

Two modes:

- **register** *(default)* — positions recorded in Spadefall's own data, world
  left untouched
- **stamp** — real structure blocks written into the world, so the map can be
  exported and shared as self-describing

### Defining a map

```
/sf map pos1                 # stand at one corner
/sf map pos2                 # stand at the opposite corner
/sf map scan getdown_1       # reads every marker in the region
```

Then wire it into an arena:

```
/sf arena create main
/sf arena maps main getdown_1 getdown_2 getdown_3
/sf arena setlobby main
```

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

## Commands

| Command | Permission |
|---|---|
| `/sf join [arena]` | `spadefall.play` |
| `/sf leave` | `spadefall.play` |
| `/sf arena create\|delete\|list\|setlobby\|maps` | `spadefall.admin` |
| `/sf map pos1\|pos2\|scan\|list\|info\|delete` | `spadefall.admin` |
| `/sf tool [role\|value\|mode]` | `spadefall.admin` |
| `/sf doctor [map]` | `spadefall.admin` |
| `/sf confirm` / `/sf cancel` | `spadefall.admin` |
| `/sf reload` | `spadefall.admin` |

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
