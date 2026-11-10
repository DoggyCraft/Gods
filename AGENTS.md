# AGENTS.md

This file provides guidance to AI coding agents working with code in this repository.

## Project

Gods is a Bukkit/Spigot plugin. Players build altars, pray to gods they name themselves, and gain powers, quests, holy lands (land protection), marriages, holy books and so on. It runs on Spigot and Paper 26.1+ with Java 25 (`api-version: '1.21'` in `plugin.yml`). It compiles against `paper-api` 26.3 but **must keep running on Spigot**: use only Bukkit/Spigot API, and reach Paper-only API behind a runtime check. WorldGuard and PlaceholderAPI are optional (softdepend).

## Build & test

```bash
nix develop                           # devshell with JDK 25 + Maven (flake.nix); or prefix commands with `nix develop -c`
mvn clean package                     # default goal; tests and builds plugin/target/Gods-<version>.jar
mvn test -pl plugin -am -Dtest=GodsTest -Dsurefire.failIfNoSpecifiedTests=false  # one test class (append #method for one test)
mvn -Pspigot -pl plugin -am clean compile  # compile against spigot-api instead: fails on any Paper-only API (CI runs this too)
```

- A parent `pom.xml` (shared versions and repositories) with the `plugin/` module (`gods`), in the standard Maven layout: `plugin/src/main/java`, `plugin/src/main/resources`, `plugin/src/test/java`. Only `plugin.yml` is Maven-filtered (for `${project.version}`).
- Java 25 (`maven.compiler.release`). CI (`.github/workflows/build.yml`) builds on JDK 25 for every push and PR. Pushing a tag `v<pom version>` (e.g. `v1.0.0`) publishes a GitHub release with the jar.
- The test suite is close to a stub. Most behavior can only be checked by running the jar on a server.
- `Biome`, `Sound` and other former enums are registry interfaces now: compare with `==`, never `switch` on them or call `valueOf`.

## Architecture

**Entry point:** `com.dogonfire.gods.Gods` (the `JavaPlugin`). `onEnable` registers the single `/g` command (alias `/gods`), loads every manager in order (`GodsConfiguration` → Permissions → Language → God → Quest → Believer → Whitelist → HolyLand if enabled), registers listeners, and starts a repeating task that calls `GodManager.instance().update()`. `onDisable` saves the managers. Add a new persistent manager to both paths.

**Singletons everywhere:** managers, `GodsConfiguration`, `GodsCommandExecuter` and `GodsTabCompleter` are lazy singletons reached through `X.instance()`. Code reaches the plugin through `Gods.instance()`. Use this pattern, not dependency injection.

**Quests:** only PILGRIMAGE, SLAY, ITEMSACRIFICE and GIVEITEMS are generated; the other `QUESTTYPE`s stay in the enum because old `quests.yml` files may hold them. Location searches (pilgrimage chests) have a hard cap on attempts and on chunks they load: never loop until a spot is found.

**Managers (`managers/`)** hold the game logic and the state. `GodManager` (~4k lines) is the core: god data, moods, powers, and the per-tick update loop. `QuestManager` and `BelieverManager` are the other large ones. Each manager loads and saves its own YAML file in the plugin data folder through `YamlConfiguration` (`gods.yml`, `believers.yml`, `quests.yml`, `marriages.yml`, `bibles.yml`, `holyland.yml`, …). Players are keyed by **UUID**; never key data by player name. Never write a raw `UUID` (or any other non-primitive object) into a `YamlConfiguration`: SnakeYAML tags it and Bukkit then refuses to load the file. Store `toString()`. `GodManager` refuses to save over a `gods.yml` that failed to load.
- `GodsConfiguration.saveSettings()` rewrites `config.yml` on every start, so a key that is saved must also be loaded, under the same spelling, or the user's value is reset.
- `HolyLandManager` stores land per 128x128 region and world: `Holylands.<world>.<hash>` / `NeutralLands.<world>.<hash>`, with `hash = ((long) x << 32) | (z & 0xFFFFFFFFL)`.
- Holy books and holy artifacts are recognized by `PersistentDataContainer` tags (`gods:holy_book_god`, `gods:holy_power`, …), never by display name or lore; old untagged items are only a fallback.
- Permission checks go through `PermissionsManager.hasPermission`, which is Bukkit's `Player#hasPermission`. There is no Vault dependency.

**Commands (`commands/`):** each subcommand extends `GodsCommand`: the constructor sets `name`, `permission`, `parameters`, `description` and `priestOnly`, and `onCommand(sender, command, args...)` gets `args[0]` = the subcommand name. `/g help` and tab completion are built from the registered commands. To add one:
1. Register it in the `GodsCommandExecuter` constructor.
2. Declare its permission node in `plugin/src/main/resources/plugin.yml` (and as a child of `gods.*`).

**Language system:** `LanguageManager` uses the `LANGUAGESTRING` enum for keys. Text comes from `lang/*.yml`:
- `english_general.yml` holds general strings.
- `<lang>_<godtype>_<gender>.yml` holds a god's voice. The file is chosen per god from its type and gender.

The files are bundled in the jar (`plugin/src/main/resources/lang/`) and copied to `plugins/Gods/lang/` when missing. A key missing from a server's copy falls back to the bundled file of the same name, then to the bundled `english_` file with the same suffix (never to another god type). A new god-voice key needs the enum entry plus a line in **all 26** `english_<type>_<gender>.yml` files; a new info key goes under `Info:` in `english_general.yml`.

**Tasks (`tasks/`):** short delayed `Runnable`s scheduled through the Bukkit scheduler (divine effects, messages, mob spawns). Repeating ones must end on their own (a stop time, the player logging off).

**Other:** `GodsPlaceholderExpansion` (PlaceholderAPI hook, registered only if that plugin is present), `listeners/` (block/chat events; `HolyLandManager` is also a listener), `changelog.txt` (release notes, newest first).
