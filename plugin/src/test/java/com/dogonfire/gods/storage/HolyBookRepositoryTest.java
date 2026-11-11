package com.dogonfire.gods.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dogonfire.gods.model.HolyBook;

class HolyBookRepositoryTest
{
	@TempDir
	Path dir;

	@Test
	void readsAnOldFile() throws IOException
	{
		Fixtures.copy("legacy/bibles-0.9.yml", this.dir, "bibles.yml");
		Storage storage = Fixtures.storage(this.dir);
		assertTrue(storage.bibles().load());
		assertEquals(3, storage.bibles().size());

		HolyBook thor = storage.bibles().find("Thor");
		assertEquals("Holy Book of Thor", thor.title());
		assertEquals("Thor", thor.author());
		assertEquals(3, thor.pages().size());
		assertEquals("Slay the ZOMBIE\nwherever you find them.", thor.pages().get(2));

		HolyBook odin = storage.bibles().find("Odin");
		assertEquals("§6The Eddas", odin.title());
		assertEquals("Hugin", odin.author());

		// A book without pages has none (they are written when it is first needed)
		assertNull(storage.bibles().find("Loki").pages());
		assertTrue(storage.bibles().find("Loki").pagesOrEmpty().isEmpty());
		storage.close();
	}

	@Test
	void savesWhatItReadAndKeepsWhatItDoesNotKnow() throws IOException
	{
		Fixtures.copy("legacy/bibles-0.9.yml", this.dir, "bibles.yml");
		Storage storage = Fixtures.storage(this.dir);
		storage.bibles().load();
		String before = Fixtures.snapshot(storage.bibles());
		storage.bibles().markDirty();
		storage.close();

		YamlConfiguration original = YamlConfiguration.loadConfiguration(Fixtures.copy("legacy/bibles-0.9.yml", this.dir, "original.yml").toFile());
		YamlConfiguration written = YamlConfiguration.loadConfiguration(this.dir.resolve("bibles.yml").toFile());
		// The same keys, and the data version (the original is kept as bibles.yml.v1.bak)
		original.set("data-version", 2);
		assertEquals(original.getKeys(true), written.getKeys(true));
		assertEquals(Files.readString(this.dir.resolve("original.yml")), Files.readString(this.dir.resolve("bibles.yml.v1.bak")));
		for (String key : original.getKeys(true))
		{
			if (!original.isConfigurationSection(key))
			{
				assertEquals(original.get(key), written.get(key), key);
			}
		}
		assertEquals(3, written.getInt("Odin.Edition"));

		Storage again = Fixtures.storage(this.dir);
		assertTrue(again.bibles().load());
		assertEquals(before, Fixtures.snapshot(again.bibles()));
		again.close();
	}

	@Test
	void aNewBookIsWrittenWithTheOldKeys() throws IOException
	{
		Storage storage = Fixtures.storage(this.dir);
		storage.bibles().load();
		HolyBook book = storage.bibles().getOrCreate("Ra");
		book.setTitle("Holy Book of Ra");
		book.setAuthor("Ra");
		book.setPages(List.of("Praise the sun"));
		storage.bibles().save();
		storage.bibles().remove("Nobody");
		storage.close();

		YamlConfiguration written = YamlConfiguration.loadConfiguration(this.dir.resolve("bibles.yml").toFile());
		assertEquals("Holy Book of Ra", written.getString("Ra.Title"));
		assertEquals("Ra", written.getString("Ra.Author"));
		assertEquals(List.of("Praise the sun"), written.getStringList("Ra.Pages"));
		assertFalse(written.contains("Nobody"));
	}
}
