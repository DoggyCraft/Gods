package com.dogonfire.gods.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Every permission a command checks is declared in plugin.yml and given by gods.* */
class PluginYmlTest {
  private static final Pattern COMMAND =
      Pattern.compile("super\\(ctx, \"([a-z]+)\", \"([a-z.]+)\"\\)");

  @Test
  void everyCommandPermissionIsDeclaredAndAChildOfGodsStar()
      throws IOException, InvalidConfigurationException {
    // Permission nodes contain dots
    YamlConfiguration flat = new YamlConfiguration();
    flat.options().pathSeparator('/');
    flat.loadFromString(Files.readString(Path.of("src/main/resources/plugin.yml")));
    assertEquals(List.of("gods"), flat.getStringList("commands/g/aliases"));
    ConfigurationSection declared = flat.getConfigurationSection("permissions");
    ConfigurationSection all = flat.getConfigurationSection("permissions/gods.*/children");

    List<String> problems = new ArrayList<>();
    int commands = 0;
    try (Stream<Path> files = Files.list(Path.of("src/main/java/com/dogonfire/gods/command"))) {
      for (Path file : files.toList()) {
        Matcher matcher = COMMAND.matcher(Files.readString(file));
        while (matcher.find()) {
          commands++;
          String node = matcher.group(2);
          if (!declared.isConfigurationSection(node)) {
            problems.add(matcher.group(1) + ": " + node + " is not declared");
          }
        }
      }
    }
    for (String node : declared.getKeys(false)) {
      if (!node.equals("gods.*") && !all.getBoolean(node)) {
        problems.add(node + " is not a child of gods.*");
      }
    }
    assertTrue(commands > 30);
    assertTrue(problems.isEmpty(), String.join(", ", problems));
  }

  /** Every /g admin action has its node, a child of gods.* and of gods.admin, for ops only */
  @Test
  void everyAdminActionHasItsNode() throws IOException, InvalidConfigurationException {
    YamlConfiguration flat = new YamlConfiguration();
    flat.options().pathSeparator('/');
    flat.loadFromString(Files.readString(Path.of("src/main/resources/plugin.yml")));
    ConfigurationSection declared = flat.getConfigurationSection("permissions");
    assertEquals("op", declared.getString("gods.admin/default"));
    for (String action : AdminArgs.ACTIONS) {
      String node = AdminArgs.permission(action);
      assertTrue(declared.isConfigurationSection(node), node + " is not declared");
      assertEquals("op", declared.getString(node + "/default"), node);
      assertTrue(
          declared.getBoolean("gods.*/children/" + node), node + " is not a child of gods.*");
      assertTrue(
          declared.getBoolean("gods.admin/children/" + node),
          node + " is not a child of gods.admin");
    }
  }
}
