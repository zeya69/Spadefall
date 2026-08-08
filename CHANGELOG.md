# Changelog

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
