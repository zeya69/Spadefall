# Changelog

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
