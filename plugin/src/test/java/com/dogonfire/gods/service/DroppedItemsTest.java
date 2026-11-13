package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DroppedItemsTest {
  private final UUID item = UUID.randomUUID();
  private final UUID player = UUID.randomUUID();

  @Test
  void aBurningItemCountsOnce() {
    DroppedItems drops = new DroppedItems();
    drops.add(this.item, this.player, 1000L);

    // Fire and lava set the item on fire again every tick it burns
    assertEquals(this.player, drops.take(this.item, 1050L));
    assertNull(drops.take(this.item, 1100L));
  }

  @Test
  void anItemNobodyDroppedIsNoSacrifice() {
    assertNull(new DroppedItems().take(this.item, 0L));
  }

  @Test
  void anItemExpiresWhenItWouldDespawn() {
    DroppedItems drops = new DroppedItems();
    drops.add(this.item, this.player, 0L);
    assertNull(drops.take(this.item, DroppedItems.LIFETIME_MILLIS + 1L));
    assertEquals(0, drops.size());
  }

  @Test
  void pruneForgetsExpiredAndGoneItems() {
    DroppedItems drops = new DroppedItems();
    UUID old = UUID.randomUUID();
    UUID gone = UUID.randomUUID();
    drops.add(old, this.player, 0L);
    drops.add(gone, this.player, 100_000L);
    drops.add(this.item, this.player, 100_000L);

    assertEquals(2, drops.prune(DroppedItems.LIFETIME_MILLIS + 1000L, id -> id.equals(gone)));
    assertEquals(1, drops.size());
    assertEquals(this.player, drops.take(this.item, DroppedItems.LIFETIME_MILLIS + 1000L));
  }
}
