package com.dogonfire.gods.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/** Test helpers for the repositories: fixture files and a storage on a temp dir */
final class Fixtures
{
	static final Logger LOG = Logger.getAnonymousLogger();

	private Fixtures()
	{
	}

	/** Copies a file from src/test/resources into {@code dir} as {@code name} */
	static Path copy(String resource, Path dir, String name) throws IOException
	{
		try (InputStream in = Fixtures.class.getResourceAsStream("/" + resource))
		{
			if (in == null)
			{
				throw new IOException("Missing test resource " + resource);
			}
			Path target = dir.resolve(name);
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
			return target;
		}
	}

	static Storage storage(Path dir)
	{
		return new Storage(dir, LOG, Executors.newSingleThreadExecutor());
	}

	/** A storage whose server knows these players (by lower case name) */
	static Storage storage(Path dir, Map<String, UUID> knownPlayers)
	{
		return new Storage(dir, LOG, Executors.newSingleThreadExecutor(), name -> knownPlayers.get(name.toLowerCase()));
	}

	/** What the repository would write now */
	static String snapshot(Repository repository)
	{
		return repository.snapshot().saveToString();
	}
}
