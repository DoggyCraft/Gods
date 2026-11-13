package com.dogonfire.gods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dogonfire.gods.model.GodMood;
import org.junit.jupiter.api.Test;

class MoodRulesTest {
  @Test
  void moodStaysWithinItsRange() {
    assertEquals(15.0F, MoodRules.add(10.0F, 5.0F));
    assertEquals(100.0F, MoodRules.add(98.0F, 5.0F));
    assertEquals(-100.0F, MoodRules.add(-98.0F, -5.0F));
  }

  @Test
  void theMoodValueDecidesHowTheGodFeels() {
    assertEquals(GodMood.ANGRY, MoodRules.moodFor(-70.5F));
    assertEquals(GodMood.DISPLEASED, MoodRules.moodFor(-70.0F));
    assertEquals(GodMood.DISPLEASED, MoodRules.moodFor(-20.5F));
    assertEquals(GodMood.NEUTRAL, MoodRules.moodFor(-20.0F));
    assertEquals(GodMood.NEUTRAL, MoodRules.moodFor(0.0F));
    assertEquals(GodMood.PLEASED, MoodRules.moodFor(20.0F));
    assertEquals(GodMood.EXALTED, MoodRules.moodFor(70.0F));
  }

  @Test
  void theSeedFixesHowMuchAGoodDeedPleases() {
    for (long seed = -50; seed < 50; seed++) {
      float pleased = MoodRules.pleasedModifier(seed);
      assertTrue(pleased >= 5.0F && pleased <= 14.0F, "seed " + seed);
      assertEquals(pleased, MoodRules.pleasedModifier(seed));
    }
  }

  @Test
  void moodFallsOffFasterWithMoreBelieversOnline() {
    // sin(0) = 0: the swing over the hours is neutral at time 0
    float one = MoodRules.falloff(7L, 0.5, 1, 0L);
    float ten = MoodRules.falloff(7L, 0.5, 10, 0L);
    assertTrue(one < 0.0F);
    assertTrue(ten < one);
    assertEquals(0.0F, MoodRules.falloff(7L, 0.0, 10, 0L), 0.0F, "no falloff in config.yml");
    assertEquals(-0.5F, MoodRules.falloff(7L, 0.5, 0, 0L));
  }
}
