package com.dogonfire.gods.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GodMoodChangeEventTest {
  @Test
  void theNewMoodStaysWithinTheMoodRange() {
    GodMoodChangeEvent event = new GodMoodChangeEvent("Thor", 10.0F, 20.0F);
    event.setNewMood(50.0F);
    assertEquals(50.0F, event.getNewMood());
    event.setNewMood(1000.0F);
    assertEquals(100.0F, event.getNewMood());
    event.setNewMood(Float.NEGATIVE_INFINITY);
    assertEquals(-100.0F, event.getNewMood());
    event.setNewMood(Float.POSITIVE_INFINITY);
    assertEquals(100.0F, event.getNewMood());
  }

  @Test
  void aNaNMoodIsRefused() {
    GodMoodChangeEvent event = new GodMoodChangeEvent("Thor", 10.0F, 20.0F);
    assertThrows(IllegalArgumentException.class, () -> event.setNewMood(Float.NaN));
    assertEquals(20.0F, event.getNewMood());
  }
}
