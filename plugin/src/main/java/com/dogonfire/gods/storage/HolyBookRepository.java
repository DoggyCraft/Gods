package com.dogonfire.gods.storage;

import com.dogonfire.gods.migration.FirstDataVersion;
import com.dogonfire.gods.migration.VersionedYaml;
import com.dogonfire.gods.model.HolyBook;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * bibles.yml: one {@link HolyBook} per god name, with the keys Gods has always written:
 *
 * <pre>
 * &lt;god name&gt;: Title, Author, Pages
 * </pre>
 *
 * plus {@code data-version: 2}. Version 1 (no {@code data-version}) has the same keys; its upgrade
 * ({@link FirstDataVersion}) makes the {@code bibles.yml.v1.bak} backup before this repository
 * first rewrites it. Keys this version doesn't know, and pages that are not a list, are kept
 * unchanged.
 */
public final class HolyBookRepository extends Repository {
  public static final int DATA_VERSION = 2;
  public static final String VERSION_KEY = "data-version";

  private static final String TITLE = "Title";
  private static final String AUTHOR = "Author";
  private static final String PAGES = "Pages";

  private static final Set<String> KNOWN = Set.of(TITLE, AUTHOR, PAGES);

  private final Map<String, HolyBook> books = new LinkedHashMap<String, HolyBook>();

  /** The data version and migrations of bibles.yml */
  static VersionedYaml format() {
    return new VersionedYaml(VERSION_KEY, DATA_VERSION, 1, List.of(new FirstDataVersion()));
  }

  HolyBookRepository(DataFile file, Logger logger) {
    super(file, logger);
  }

  /** The god's holy book, or null */
  public HolyBook find(String godName) {
    return (godName != null) ? this.books.get(godName) : null;
  }

  /** The god's holy book, created (empty) if there is none */
  public HolyBook getOrCreate(String godName) {
    return this.books.computeIfAbsent(godName, HolyBook::new);
  }

  public void remove(String godName) {
    this.books.remove(godName);
  }

  public Collection<HolyBook> all() {
    return Collections.unmodifiableCollection(this.books.values());
  }

  @Override
  public int size() {
    return this.books.size();
  }

  @Override
  protected void clear() {
    this.books.clear();
  }

  @Override
  protected void read(YamlConfiguration yaml) {
    for (String key : yaml.getKeys(false)) {
      ConfigurationSection section = yaml.getConfigurationSection(key);
      if (key.equals(VERSION_KEY)) {
        continue;
      }
      if (section == null) {
        keepUnknown(key, yaml.get(key));
        continue;
      }

      HolyBook book = new HolyBook(key);
      Map<String, Object> extras = extraKeys(section, KNOWN);
      book.setTitle(section.getString(TITLE));
      book.setAuthor(section.getString(AUTHOR));
      if (section.isList(PAGES)) {
        book.setPages(section.getStringList(PAGES));
      } else if (section.contains(PAGES)) {
        extras.put(PAGES, toPlain(section.get(PAGES)));
      }
      book.setExtras(extras);
      this.books.put(key, book);
    }

    logger().info("Loaded " + this.books.size() + " bibles.");
  }

  @Override
  protected void write(YamlConfiguration yaml) {
    for (HolyBook book : this.books.values()) {
      ConfigurationSection section = yaml.createSection(book.godName());
      section.set(TITLE, book.title());
      section.set(AUTHOR, book.author());
      if (book.pages() != null) {
        section.set(PAGES, book.pages());
      }
      writeExtras(section, book.extras());
    }
  }
}
