# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Gods is a Bukkit/Spigot plugin. Players build altars, pray to gods they name themselves, and gain powers, quests, holy lands (land protection), marriages, holy books and so on. It targets Spigot API 1.14.4 (`api-version: 1.13` in `plugin.yml`) and Java 8. WorldGuard, Vault, PlaceholderAPI and the permission plugins are optional (softdepend).

## Build & test

```bash
mvn clean package                 # builds target/Gods-<version>.jar (what CI runs)
mvn                               # default goal: clean package cobertura:cobertura
mvn test                          # JUnit 5 tests (src/test/java)
mvn -Dtest=GodsTest#testTrue test # run a single test
mvn cobertura:cobertura           # coverage report in target/site/cobertura/
```

- The Maven `sourceDirectory` and resource directory are both `src/` (not `src/main/java`). Resources are **filtered**, so `${project.version}` in `src/plugin.yml` is replaced at build time.
- Several Maven repos in `pom.xml` use plain `http://` URLs. Newer Maven versions block these, so dependency resolution may fail.
- The test suite (`src/test/java/GodsTest.java`, package `test.java`) is close to a stub. Most behavior can only be checked by running the jar on a Spigot server.

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

**Tasks (`tasks/`):** short delayed `Runnable`s scheduled through the Bukkit scheduler (divine effects, messages, mob spawns). The directory has duplicate pairs such as `TaskHealPlayer` / `HealPlayerTask`. Only the `Task*`-prefixed classes are used. The `*Task` versions are legacy. The same applies to `com.dogonfire.gods.ChatManager`; the live one is `managers.ChatManager`.

**Other:** `GodsPlaceholderExpansion` (PlaceholderAPI hook, registered only if that plugin is present), `listeners/` (block/chat events; `HolyLandManager` is also a listener), `Metrics` (bStats), `src/changelog.txt` (release notes, newest first).
