package com.dogonfire.gods.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PrayForCommandTest
{
	@Test
	void prayersOffTurnsOffTheCommand()
	{
		assertEquals("command.disabled.prayers", PrayForCommand.disabledFor(null, false, true, true));
		assertEquals("command.disabled.prayers", PrayForCommand.disabledFor("quest", false, true, true));
		assertEquals("command.disabled.prayers", PrayForCommand.disabledFor("item", false, true, true));
	}

	@Test
	void theCommandNeedsSomethingToPrayFor()
	{
		assertNull(PrayForCommand.disabledFor(null, true, true, false));
		assertNull(PrayForCommand.disabledFor(null, true, false, true));
		assertEquals("command.disabled.blessings", PrayForCommand.disabledFor(null, true, false, false));
	}

	@Test
	void eachKindNeedsItsOwnFeature()
	{
		for (String kind : new String[] { "item", "health", "blessing", "ITEM" })
		{
			assertNull(PrayForCommand.disabledFor(kind, true, true, false));
			assertEquals("command.disabled.blessings", PrayForCommand.disabledFor(kind, true, false, true));
		}
		assertNull(PrayForCommand.disabledFor("quest", true, false, true));
		assertEquals("command.disabled.quests", PrayForCommand.disabledFor("quest", true, true, false));

		// Anything else shows the help
		assertNull(PrayForCommand.disabledFor("", true, true, true));
	}
}
