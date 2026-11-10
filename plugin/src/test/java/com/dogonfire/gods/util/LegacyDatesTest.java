package com.dogonfire.gods.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

class LegacyDatesTest
{
	@Test
	void roundTripsTheFileFormats()
	{
		assertEquals("12:34:56 01-02-2024", LegacyDates.SECONDS.format(LegacyDates.SECONDS.parse("12:34:56 01-02-2024")));
		assertEquals("12:34 01-02-2024", LegacyDates.MINUTES.format(LegacyDates.MINUTES.parse("12:34 01-02-2024")));
	}

	@Test
	void unreadableTimesAreNull()
	{
		assertNull(LegacyDates.SECONDS.parse(null));
		assertNull(LegacyDates.SECONDS.parse("yesterday"));
		assertNull(LegacyDates.SECONDS.parse("12:34 01-02-2024"));
		assertNull(LegacyDates.SECONDS.format(null));
	}

	@Test
	void nowHasThePrecisionOfTheFile()
	{
		Instant now = LegacyDates.MINUTES.now();
		assertEquals(now, now.truncatedTo(ChronoUnit.MINUTES));
		assertEquals(now, LegacyDates.MINUTES.parse(LegacyDates.MINUTES.format(now)));
		Instant seconds = LegacyDates.SECONDS.now();
		assertEquals(seconds, LegacyDates.SECONDS.parse(LegacyDates.SECONDS.format(seconds)));
	}
}
