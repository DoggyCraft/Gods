# AGENTS.md

This file provides guidance to AI coding agents working with code in this repository.

## Project

Gods is a Bukkit/Spigot plugin. Players build altars, pray to gods they name themselves, and gain powers, quests, holy lands (land protection), marriages, holy books and so on. It runs on Spigot and Paper 26.1+ with Java 25 (`api-version: '1.21'` in `plugin.yml`). It compiles against `paper-api` 26.3 but **must keep running on Spigot**: use only Bukkit/Spigot API, and reach Paper-only API behind a runtime check. WorldGuard, Vault, PlaceholderAPI and the permission plugins are optional (softdepend).

## Build & test

```bash
nix develop                           # devshell with JDK 25 + Maven (flake.nix); or prefix commands with `nix develop -c`
mvn clean package                     # default goal; tests and builds target/Gods-<version>.jar
mvn test -Dtest=GodsTest              # one test class (append #method for one test)
mvn -Pspigot clean compile            # compile against spigot-api instead: fails on any Paper-only API (CI runs this too)
```

- Standard Maven layout: `src/main/java`, `src/main/resources`, `src/test/java`. Only `plugin.yml` is Maven-filtered (for `${project.version}`).
- Java 25 (`maven.compiler.release`). CI (`.github/workflows/build.yml`) builds on JDK 25 for every push and PR. Pushing a tag `v<pom version>` (e.g. `v1.0.0`) publishes a GitHub release with the jar.
- The test suite is close to a stub. Most behavior can only be checked by running the jar on a server.
- `Biome`, `Sound` and other former enums are registry interfaces now: compare with `==`, never `switch` on them or call `valueOf`.

## Architecture

**Entry point:** `com.dogonfire.gods.Gods` (the `JavaPlugin`). `onEnable` registers the single `/g` command (alias `/gods`), loads every manager in order (`GodsConfiguration` → Permissions → Language → God → Quest → Believer → Whitelist → HolyLand if enabled), registers listeners, and starts a repeating task that calls `GodManager.instance().update()`. `onDisable` saves the managers. Add a new persistent manager to both paths.

**Singletons everywhere:** managers, `GodsConfiguration`, `GodsCommandExecuter` and `GodsTabCompleter` are lazy singletons reached through `X.instance()`. Code reaches the plugin through `Gods.instance()`. Use this pattern, not dependency injection.

**Managers (`managers/`)** hold the game logic and the state. `GodManager` (~4k lines) is the core: god data, moods, powers, and the per-tick update loop. `QuestManager` and `BelieverManager` are the other large ones. Each manager loads and saves its own YAML file in the plugin data folder through `YamlConfiguration` (`gods.yml`, `believers.yml`, `marriages.yml`, `bibles.yml`, `holylaws.yml`, `holyartifacts.yml`, …). Players are keyed by **UUID**. Recent work has moved calls from player names to UUIDs, so keep using UUIDs.

**Commands (`commands/`):** each subcommand extends `GodsCommand` (it sets `name` and `permission` in the constructor and implements `onCommand(sender, command, args...)`). To add one:
1. Register it in the `GodsCommandExecuter` constructor.
2. Add it to the hardcoded, permission-gated lists in `GodsTabCompleter`.
3. Declare its permission node in `src/plugin.yml`.

**Language system:** `LanguageManager` uses the `LANGUAGESTRING` enum for keys. Text comes from `lang/*.yml`:
- `english_general.yml` holds general strings.
- `<lang>_<godtype>_<gender>.yml` holds a god's voice. The file is chosen per god from its type and gender.

At runtime, missing language files are **downloaded from `raw.githubusercontent.com/DogOnFire/Gods/master/lang/`**, not bundled in the jar. A new key needs the enum entry, plus the string in every relevant `lang/` file on master.

**Tasks (`tasks/`):** short delayed `Runnable`s scheduled through the Bukkit scheduler (divine effects, messages, mob spawns).

**Other:** `GodsPlaceholderExpansion` (PlaceholderAPI hook, registered only if that plugin is present), `listeners/` (block/chat events; `HolyLandManager` is also a listener), `changelog.txt` (release notes, newest first).
