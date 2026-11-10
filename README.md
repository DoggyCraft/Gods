Gods
======

[![Build](https://github.com/DoggyCraft/Gods/actions/workflows/build.yml/badge.svg)](https://github.com/DoggyCraft/Gods/actions/workflows/build.yml)

# ![Gods](img/Gods.jpg)

The plugin for adding Gods to minecraft!

A God is an entity that exist because a player has chosen to believe in it.

A player shows his faith in a god by building an altar for his god and using the altar to pray. 
In this way, many gods may exist at a time, all with names created by the players.

The main purpose for believing in these Gods is to gain from the help the Gods provide (see below) and to provide some basic faction play based on ideology and culture (I.e. "My god is better than yours because apples are holy!").

Another purpose is simply to get people inspired to build awesome churches and use them for their Gods.

With Holy Lands, players can easily protect their lands from grief by simply building alters to their Gods.


Installation
---------
*	Run a [Paper](https://papermc.io/) or Spigot server on Minecraft 26.1 or newer, with Java 25.
*	Download `Gods-<version>.jar` from the [releases](https://github.com/DoggyCraft/Gods/releases) and drop it into the `plugins` folder.
*	Optional: [Vault](https://www.spigotmc.org/resources/vault.34315/), [WorldGuard](https://enginehub.org/worldguard), [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/).
*	Start the server. Commands and permissions are listed in [plugin.yml](plugin/src/main/resources/plugin.yml).

Links
---------

*	[Bukkit Plugin Page](https://dev.bukkit.org/bukkit-plugins/gods/)
*	[Releases](https://github.com/DoggyCraft/Gods/releases)

Building
---------
Gods is built with Maven and JDK 25:

```bash
git clone https://github.com/DoggyCraft/Gods.git
cd Gods
mvn clean package        # or: nix develop -c mvn clean package
```

The plugin jar ends up in `plugin/target/Gods-<version>.jar`.
