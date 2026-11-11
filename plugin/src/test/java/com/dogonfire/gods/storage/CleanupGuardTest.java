package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dogonfire.gods.storage.CleanupGuard.Verdict;

class CleanupGuardTest
{
	private static final Duration	LATER	= CleanupGuard.GRACE_PERIOD.plusMinutes(1);

	@TempDir
	Path							dir;

	@Test
	void allowsRemovingWhenEverythingWasReadAndTheGracePeriodIsOver()
	{
		assertEquals(Verdict.ALLOWED, CleanupGuard.check(false, 3, 10, LATER));
		assertEquals(Verdict.ALLOWED, CleanupGuard.check(false, 0, 0, LATER));
	}

	@Test
	void neverRemovesWhileAFileCouldNotBeRead()
	{
		assertEquals(Verdict.FILE_NOT_READABLE, CleanupGuard.check(true, 3, 10, LATER));
		assertEquals(Verdict.FILE_NOT_READABLE, CleanupGuard.check(true, 3, 0, LATER));
	}

	@Test
	void neverRemovesWhenThereAreGodsButNoBelievers()
	{
		assertEquals(Verdict.NO_BELIEVERS, CleanupGuard.check(false, 3, 0, LATER));
	}

	@Test
	void waitsAfterALoad()
	{
		assertEquals(Verdict.GRACE_PERIOD, CleanupGuard.check(false, 3, 10, null));
		assertEquals(Verdict.GRACE_PERIOD, CleanupGuard.check(false, 3, 10, Duration.ofSeconds(5)));
	}

	@Test
	void aBrokenBelieversFileStopsTheDeadGodSweep() throws IOException
	{
		Files.writeString(this.dir.resolve("gods.yml"), "data-version: 2\nThor:\n  Created: 10:00:00 01-01-2019\n");
		Files.writeString(this.dir.resolve("believers.yml"), "8667ba71-b85a-4004-af54-457a9734eed7:\n  God: [unclosed\n");
		Storage storage = Fixtures.storage(this.dir);
		storage.gods().load();
		storage.believers().load();
		storage.dataLoaded();

		assertTrue(storage.believers().isReadOnly());
		assertEquals(Verdict.FILE_NOT_READABLE, storage.cleanupVerdict());
		storage.close();
	}

	@Test
	void aMissingBelieversFileStopsTheDeadGodSweep() throws IOException
	{
		Files.writeString(this.dir.resolve("gods.yml"), "data-version: 2\nThor:\n  Created: 10:00:00 01-01-2019\n");
		Storage storage = Fixtures.storage(this.dir);
		storage.gods().load();
		storage.believers().load();
		storage.dataLoaded();

		assertEquals(Verdict.NO_BELIEVERS, storage.cleanupVerdict());
		storage.close();
	}

	@Test
	void nothingIsRemovedRightAfterLoading() throws IOException
	{
		Files.writeString(this.dir.resolve("gods.yml"), "data-version: 2\nThor:\n  Created: 10:00:00 01-01-2019\n");
		Files.writeString(this.dir.resolve("believers.yml"), "8667ba71-b85a-4004-af54-457a9734eed7:\n  God: Thor\n");
		Storage storage = Fixtures.storage(this.dir);
		storage.gods().load();
		storage.believers().load();
		storage.dataLoaded();

		assertEquals(Verdict.GRACE_PERIOD, storage.cleanupVerdict());
		storage.close();
	}
}
