package com.dogonfire.gods.integration.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dogonfire.gods.config.Settings.NpcProvider;
import org.junit.jupiter.api.Test;

class NpcProvidersTest {
  private static NpcProvider auto(boolean citizens, boolean fancyNpcs, boolean mannequin) {
    NpcProviders.Choice choice =
        NpcProviders.choose(true, NpcProvider.AUTO, citizens, fancyNpcs, mannequin);
    assertNull(choice.warning());
    return choice.provider();
  }

  /** Citizens first, then FancyNpcs, then the mannequin, then none */
  @Test
  void autoPrefersThePluginsThenTheMannequin() {
    assertEquals(NpcProvider.CITIZENS, auto(true, true, true));
    assertEquals(NpcProvider.CITIZENS, auto(true, false, false));
    assertEquals(NpcProvider.FANCYNPCS, auto(false, true, true));
    assertEquals(NpcProvider.FANCYNPCS, auto(false, true, false));
    assertEquals(NpcProvider.BUILTIN, auto(false, false, true));
    assertEquals(NpcProvider.OFF, auto(false, false, false));
  }

  @Test
  void aNamedProviderIsUsedWhenThere() {
    assertEquals(
        NpcProvider.BUILTIN,
        NpcProviders.choose(true, NpcProvider.BUILTIN, true, true, true).provider());
    assertEquals(
        NpcProvider.FANCYNPCS,
        NpcProviders.choose(true, NpcProvider.FANCYNPCS, true, true, true).provider());
    assertEquals(
        NpcProvider.CITIZENS,
        NpcProviders.choose(true, NpcProvider.CITIZENS, true, false, false).provider());
  }

  /** The admin asked for that one: no other instead, but a warning */
  @Test
  void aNamedProviderTheServerLacksGivesNoneWithAWarning() {
    for (NpcProvider wanted :
        new NpcProvider[] {NpcProvider.BUILTIN, NpcProvider.CITIZENS, NpcProvider.FANCYNPCS}) {
      NpcProviders.Choice choice = NpcProviders.choose(true, wanted, false, false, false);
      assertEquals(NpcProvider.OFF, choice.provider(), wanted.name());
      assertNotNull(choice.warning(), wanted.name());
    }
  }

  @Test
  void offOrDisabledGivesNone() {
    assertEquals(
        new NpcProviders.Choice(NpcProvider.OFF, null),
        NpcProviders.choose(true, NpcProvider.OFF, true, true, true));
    for (NpcProvider wanted : NpcProvider.values()) {
      assertEquals(
          new NpcProviders.Choice(NpcProvider.OFF, null),
          NpcProviders.choose(false, wanted, true, true, true),
          wanted.name());
    }
  }
}
