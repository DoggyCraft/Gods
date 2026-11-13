package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GodListRepositoryTest {
  @TempDir Path dir;

  @Test
  void readsTheWhitelist() throws IOException {
    Files.writeString(
        this.dir.resolve("whitelist.yml"),
        "Thor:\n  MinPower: 25\nOdin:\n  MinPower: lots\nLoki: ''\n");
    Storage storage = Fixtures.storage(this.dir);
    assertTrue(storage.whitelist().load());

    assertTrue(storage.whitelist().contains("Thor"));
    assertTrue(storage.whitelist().contains("Loki"));
    assertFalse(storage.whitelist().contains("Frigg"));
    assertEquals(25, storage.whitelist().minPower("Thor"));
    assertEquals(0, storage.whitelist().minPower("Odin"), "not a number");
    assertEquals(0, storage.whitelist().minPower("Loki"));
    assertEquals(0, storage.whitelist().minPower("Frigg"));
    storage.close();
  }

  @Test
  void writesTheFileBackAsItWas() throws IOException {
    String content = "Thor:\n  MinPower: 25\nLoki: ''\n";
    Files.writeString(this.dir.resolve("blacklist.yml"), content);
    Storage storage = Fixtures.storage(this.dir);
    storage.blacklist().load();
    assertTrue(storage.blacklist().contains("Loki"));
    storage.blacklist().markDirty();
    storage.close();

    assertEquals(content, Files.readString(this.dir.resolve("blacklist.yml")));
  }
}
