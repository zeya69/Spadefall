# Changelog

## [0.4.0] - bundled map + schematic loader

### Added
- **Mount St. Helens**, a generated crater demo map bundled in the jar:
  glacier rim, terraced ash and basalt walls, a blown-out north breach, lava
  seams, and a lava lake the finish line sits above. 24 starts, 73 chips,
  13 spades, 8 deathmatch spawns. Generator kept in `tools/`
- `/sf setup demo` and a hub item: paste it where you stand and get a ready
  arena named `demo`
- Built-in Sponge schematic reader (v1-v3) and a tick-budgeted paster - no
  WorldEdit dependency. Markers in the file are read, not placed
- `/sf map load <file> [name]` for any schematic
- `docs/MAP-BUILDERS-GUIDE.md` - the spec for commissioning or submitting maps

### Fixed
- Finish detection now checks the *crossing* of the line, so players at
  terminal velocity can't fall through it
- Lava outside the finish lake sends you back to your start instead of being
  harmless

## [0.3.0] - slice 2: the game

### Added
- The descent: scatter to starts, freeze, GO, finish line as a Y level,
  placement points, timeout points, per-map standings, breather, results
- Chip pickups drawn from the map's pool, poker-coloured by value, collected
  by walking through; score and unbanked chips both rise
- Spectator treatment for finished players: spectator mode, hidden from
  active players, chat kept between spectators
- Inventory / gamemode / location / health / XP save on join and restore on
  leave, written to disk for crash recovery
- Lobby and in-round protections; fall damage OFF / ON / CAPPED (default
  capped, never lethal); void fall-out sends you back to your start
- Per-arena overrides for min-players, auto-start and countdown, in config,
  via /sf arena set, and in the setup GUI's Settings item
- /sf leave, /sf start (force), /sf stop (hard stop and restore everyone)
- Sidebar scoreboard; chips banked and stats recorded at round end
- Finish line drawn in glow mode as a red outline at finish height
- Quit cleanup: disconnecting leaves the arena properly

### Fixed
- Countdown stub that spammed every second past zero
- /sf join on your own arena no longer leaves and rejoins

## [0.2.0] - slice 1.5: setup experience

### Added
- `/sf setup` hub and arena setup inventory - one item per step, coloured by
  status, click to act. Register unlocks only when nothing is red
- Edit mode with per-player **marker glow**: display entities colour-coded by
  role with floating labels (`Start #3`, `Chip · 25`), region drawn as white
  edges. Hidden from everyone not editing; swept on startup if a crash left any
- `/sf next` (one step, clickable buttons) and `/sf missing` (full checklist
  with [Fix] buttons)
- Chat wizard mirror of the GUI, built on the BungeeCord component API so it
  works on Spigot and Paper
- Auto region: bounding box of markers plus configurable padding. Manual wand
  still available
- Maps are now live drafts persisted on every change, with a `registered` flag;
  arenas disable themselves until every map is registered
- Multi-map arenas editable from the GUI (cycle / add)
- Import stamped structure-block markers from a region
- First-run hint for ops when no arenas exist
- Tool: role shown in item name, counts in every message, `/sf tool status`,
  shift+scroll to cycle roles

### Changed
- "spawn" is now "Player Start" everywhere players read it. File format unchanged
- Plain left-click with the tool now reports what's there; removal is
  shift+left-click, because accidental removal was too easy

## [Unreleased] - slice 1

### Added
- Plugin skeleton targeting Paper 26.2 / Java 25
- `ConfigManager` and `Messages` with hex colour support
- SQLite (default) and MySQL storage behind HikariCP, with schema creation
  and daily match-history pruning
- `ArenaManager` and the one-second round phase machine
- `WaitingPhase` and `CountdownPhase`, including cancel-on-underflow and
  shorten-when-full
- Self-describing map format: structure-block markers with roles and
  denominations
- `MarkerScanner` — tick-budgeted region scan that never generates terrain
- `MapValidator` — structural errors refuse a map, density warnings are
  always overridable
- Marker tool (golden shovel) with register and stamp modes
- `/sf doctor` and the `/sf confirm` / `/sf cancel` prompt
- GitHub Actions build, attaching the jar to tagged releases
