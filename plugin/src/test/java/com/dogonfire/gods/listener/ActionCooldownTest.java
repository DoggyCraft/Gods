package com.dogonfire.gods.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Test;

class ActionCooldownTest {
  private static final class TestEvent extends Event {
    @Override
    public HandlerList getHandlers() {
      return null;
    }
  }

  @Test
  void everyListenerGetsTheSameAnswerForOneEvent() {
    ActionCooldown cooldown = new ActionCooldown();
    UUID player = UUID.randomUUID();

    Event meal = new TestEvent();
    assertTrue(cooldown.ready(player, meal));
    assertTrue(cooldown.ready(player, meal), "the second listener for the same meal");

    Event nextMeal = new TestEvent();
    assertFalse(cooldown.ready(player, nextMeal), "within 10 seconds");
    assertFalse(cooldown.ready(player, nextMeal));

    assertTrue(cooldown.ready(UUID.randomUUID(), new TestEvent()), "another player");

    cooldown.forget(player);
    assertTrue(cooldown.ready(player, new TestEvent()), "after leaving");
  }
}
