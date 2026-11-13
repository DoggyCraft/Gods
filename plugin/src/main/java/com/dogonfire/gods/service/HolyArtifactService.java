package com.dogonfire.gods.service;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.message.Messages;
import com.dogonfire.gods.model.GodType;
import com.dogonfire.gods.model.HolyPower;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Holy artifacts: items a god gives to a believer (a blessing, or /g giveartifact). Right-clicking
 * one activates its holy power ({@link HolyPowerService}). An artifact keeps its state (power,
 * uses, name) in its own persistent data ({@code gods:holy_power}, {@code gods:holy_power_value},
 * {@code gods:holy_artifact_uses}, {@code gods:holy_artifact_name}), so it needs no data file;
 * artifacts from before the tags are recognized by their lore. Never by display name. Main thread
 * only.
 */
public class HolyArtifactService {
  /**
   * The power of a god type's artifacts and the items they can be; their names and lore are
   * artifact.kind.<TYPE> in messages.yml
   */
  private record ArtifactKind(HolyPower power, List<Material> items) {
    ArtifactKind(HolyPower power, Material... items) {
      this(power, List.of(items));
    }
  }

  private final Random random = new Random();

  private final NamespacedKey powerKey;
  private final NamespacedKey powerValueKey;
  private final NamespacedKey usesKey;
  private final NamespacedKey nameKey;

  private final GodsContext ctx;

  public HolyArtifactService(GodsContext ctx) {
    this.ctx = ctx;
    this.powerKey = new NamespacedKey(ctx.plugin(), "holy_power");
    this.powerValueKey = new NamespacedKey(ctx.plugin(), "holy_power_value");
    this.usesKey = new NamespacedKey(ctx.plugin(), "holy_artifact_uses");
    this.nameKey = new NamespacedKey(ctx.plugin(), "holy_artifact_name");
  }

  private static ArtifactKind getArtifactKind(GodType godType) {
    return switch (godType) {
      case FROST -> new ArtifactKind(HolyPower.FREEZE, Material.STICK, Material.IRON_SWORD);
      case LOVE -> new ArtifactKind(HolyPower.HEALING, Material.DIAMOND, Material.AMETHYST_SHARD);
      case EVIL -> new ArtifactKind(HolyPower.LIGHTNING, Material.IRON_SWORD, Material.IRON_AXE);
      case SEA ->
          new ArtifactKind(HolyPower.ICE, Material.HEART_OF_THE_SEA, Material.PRISMARINE_SHARD);
      case MOON -> new ArtifactKind(HolyPower.CALLMOON, Material.CLOCK, Material.PHANTOM_MEMBRANE);
      case SUN -> new ArtifactKind(HolyPower.CALLSUN, Material.BLAZE_ROD, Material.GOLDEN_SWORD);
      case THUNDER ->
          new ArtifactKind(HolyPower.LIGHTNING_STORM, Material.STICK, Material.GOLDEN_AXE);
      case PARTY -> new ArtifactKind(HolyPower.FIREWORK, Material.STICK, Material.FIREWORK_STAR);
      case WAR -> new ArtifactKind(HolyPower.FIREBALL, Material.IRON_SWORD, Material.IRON_AXE);
      case WEREWOLVES ->
          new ArtifactKind(HolyPower.TAME, Material.RABBIT_FOOT, Material.IRON_SWORD);
      case CREATURES -> new ArtifactKind(HolyPower.TAME, Material.STICK, Material.FEATHER);
      case WISDOM -> new ArtifactKind(HolyPower.KNOWLEDGE, Material.BOOK, Material.FEATHER);
      case NATURE -> new ArtifactKind(HolyPower.NATURE, Material.GOLDEN_HOE, Material.STICK);
    };
  }

  /** The second lore line: how often the artifact was used */
  private String usesLine(int uses) {
    return ctx.messages()
        .legacy(uses == 1 ? "artifact.used-once" : "artifact.used", Messages.amount(uses));
  }

  public Item createHolyArtifact(GodType godType, String godName, Location location) {
    if (godType == null) {
      ctx.plugin().log("createHolyArtifact(): " + godName + " has no god type");
      return null;
    }

    ArtifactKind kind = getArtifactKind(godType);
    int index = this.random.nextInt(kind.items().size());
    Material material = kind.items().get(index);
    HolyPower holyPower = kind.power();
    // As strong as the giving god's rank is now; the item keeps it
    int powerValue = HolyPowerRules.strength(holyPower, ctx.rankService().getRank(godName));
    Messages messages = ctx.messages();
    List<String> names = messages.raw("artifact.kind." + godType.name() + ".items");
    Component baseName =
        messages.parse(names.isEmpty() ? material.name() : names.get(index % names.size()));
    String itemName =
        Messages.plain(
            messages.component(
                "artifact.item-name", Messages.component("item", baseName), Messages.god(godName)));

    ItemStack item = new ItemStack(material);
    ItemMeta itemMeta = item.getItemMeta();
    if (itemMeta == null) {
      ctx.plugin().logDebug("createHolyArtifact(): Could not get item meta for " + material);
      return null;
    }

    itemMeta.setDisplayName(
        messages.legacy("artifact.display-name", Messages.text("name", itemName)));

    // Three lines, as before: the kind, the uses and the power
    List<String> lore = new ArrayList<String>();
    lore.add(messages.legacy("artifact.kind." + godType.name() + ".lore"));
    lore.add(usesLine(0));
    lore.add(
        messages.legacy(
            "artifact.power." + holyPower.name(),
            Messages.text("value", powerValue),
            Messages.text("double", 2 * powerValue),
            Messages.text("radius", HolyPowerRules.effectRadius(powerValue)),
            Messages.text("distance", HolyPowerRules.teleportDistance(powerValue)),
            Messages.text("hearts", (int) (HolyPowerRules.earthquakeDamage(powerValue) / 2))));
    itemMeta.setLore(lore);

    switch (this.random.nextInt(6)) {
      case 0 -> itemMeta.addEnchant(Enchantment.SHARPNESS, 1 + this.random.nextInt(2), true);
      case 1 -> itemMeta.addEnchant(Enchantment.SMITE, 1 + this.random.nextInt(2), true);
      case 2 -> itemMeta.addEnchant(Enchantment.FIRE_ASPECT, 1 + this.random.nextInt(2), true);
      case 3 -> itemMeta.addEnchant(Enchantment.KNOCKBACK, 1 + this.random.nextInt(2), true);
      case 4 -> itemMeta.addEnchant(Enchantment.EFFICIENCY, 1 + this.random.nextInt(2), true);
      default -> itemMeta.addEnchant(Enchantment.LOOTING, 1 + this.random.nextInt(2), true);
    }

    PersistentDataContainer data = itemMeta.getPersistentDataContainer();
    data.set(this.powerKey, PersistentDataType.STRING, holyPower.name());
    data.set(this.powerValueKey, PersistentDataType.INTEGER, powerValue);
    data.set(this.usesKey, PersistentDataType.INTEGER, 0);
    data.set(this.nameKey, PersistentDataType.STRING, itemName);

    item.setItemMeta(itemMeta);

    return location.getWorld().dropItem(location, item);
  }

  /**
   * Activates the holy power of the artifact a player right-clicked with. Each use counts towards
   * the artifact's rank, and wears it like any other use of a tool.
   */
  public void handleActivate(Player player, ItemStack item) {
    if (!isHolyArtifact(item)) {
      return;
    }
    HolyPowerRules.PowerDescription holyPower = getHolyPower(item);
    if (holyPower == null) {
      return;
    }

    if (!ctx.holyPowerService().activatePower(player, holyPower.power(), holyPower.value())) {
      return;
    }

    ItemMeta itemMeta = item.getItemMeta();
    PersistentDataContainer data = itemMeta.getPersistentDataContainer();

    // Tag artifacts from before the persistent data, so they keep working
    // if their lore is changed
    if (!data.has(this.powerKey, PersistentDataType.STRING)) {
      data.set(this.powerKey, PersistentDataType.STRING, holyPower.power().name());
      data.set(this.powerValueKey, PersistentDataType.INTEGER, holyPower.value());
      data.set(
          this.nameKey, PersistentDataType.STRING, ChatColor.stripColor(itemMeta.getDisplayName()));
    }

    int oldUses = data.getOrDefault(this.usesKey, PersistentDataType.INTEGER, 0);
    int uses = oldUses + 1;
    data.set(this.usesKey, PersistentDataType.INTEGER, uses);

    List<String> lore = itemMeta.getLore();
    if (lore != null && lore.size() >= 2) {
      lore.set(1, usesLine(uses));
      itemMeta.setLore(lore);
    }

    String rank = HolyArtifactRules.rank(uses);
    if (!rank.equals(HolyArtifactRules.rank(oldUses))) {
      String baseName =
          data.getOrDefault(
              this.nameKey,
              PersistentDataType.STRING,
              ChatColor.stripColor(itemMeta.getDisplayName()));
      Component rankName = ctx.messages().component("artifact.rank." + rank);
      itemMeta.setDisplayName(
          ctx.messages()
              .legacy(
                  "artifact.ranked-name",
                  Messages.component("rank", rankName),
                  Messages.text("name", baseName)));
      ctx.messages()
          .broadcast(
              ctx.plugin().getServer(),
              "artifact.ranked",
              Messages.player(player.getName()),
              Messages.text("name", baseName),
              Messages.component("rank", rankName));
    }

    boolean broken = false;
    if (itemMeta instanceof Damageable damageable && !itemMeta.isUnbreakable()) {
      int maxDamage =
          damageable.hasMaxDamage() ? damageable.getMaxDamage() : item.getType().getMaxDurability();
      if (maxDamage > 0) {
        int damage = damageable.getDamage() + 1;
        if (damage >= maxDamage) {
          broken = true;
        } else {
          damageable.setDamage(damage);
        }
      }
    }

    item.setItemMeta(itemMeta);

    if (broken) {
      item.setAmount(item.getAmount() - 1);
      player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
    }
  }

  /** True if the player carries a Holy artifact anywhere in their inventory. */
  public boolean hasHolyArtifact(Player player) {
    for (ItemStack item : player.getInventory().getContents()) {
      if (isHolyArtifact(item)) {
        return true;
      }
    }
    return false;
  }

  public boolean isHolyArtifact(ItemStack item) {
    if (item == null || !item.hasItemMeta()) {
      return false;
    }
    ItemMeta itemMeta = item.getItemMeta();
    if (itemMeta.getPersistentDataContainer().has(this.powerKey, PersistentDataType.STRING)) {
      return true;
    }
    return getLegacyHolyPower(itemMeta) != null;
  }

  /**
   * The power of an artifact, from its persistent data or, for artifacts made by older versions,
   * from its lore. Null if the item is no artifact.
   */
  private HolyPowerRules.PowerDescription getHolyPower(ItemStack item) {
    ItemMeta itemMeta = item.getItemMeta();
    PersistentDataContainer data = itemMeta.getPersistentDataContainer();
    String powerName = data.get(this.powerKey, PersistentDataType.STRING);
    if (powerName == null) {
      return getLegacyHolyPower(itemMeta);
    }
    try {
      HolyPower power = HolyPower.valueOf(powerName);
      return new HolyPowerRules.PowerDescription(
          power, data.getOrDefault(this.powerValueKey, PersistentDataType.INTEGER, 1));
    } catch (IllegalArgumentException ex) {
      ctx.plugin().logDebug("Unknown holy power '" + powerName + "' on a Holy artifact");
      return null;
    }
  }

  /**
   * Reads the power of an artifact made before artifacts were tagged: its lore starts with the
   * artifact line and its third line describes a holy power. Null for any other item.
   */
  private HolyPowerRules.PowerDescription getLegacyHolyPower(ItemMeta itemMeta) {
    if (!itemMeta.hasLore()) {
      return null;
    }
    List<String> lore = itemMeta.getLore();
    if (lore.size() < 3) {
      return null;
    }
    String firstLine = ChatColor.stripColor(lore.get(0));
    if (!HolyArtifactRules.isArtifactLine(firstLine)) {
      return null;
    }
    return HolyPowerRules.parse(ChatColor.stripColor(lore.get(2)));
  }
}
