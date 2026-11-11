package com.dogonfire.gods.storage;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Owns the data files in the plugin folder and the one writer thread they share (one thread, so writes land in the
 * order they were made). Every file (gods.yml, believers.yml, quests.yml, holyland.yml, marriages.yml, bibles.yml,
 * whitelist.yml, blacklist.yml and altars.yml) is held as models by a {@link Repository} (loaded and changed by its service), over a
 * {@link DataFile}. A change marks the file dirty;
 * {@link #flush()} queues a write for every changed file (main thread: on the autosave timer, on /g reload and on
 * disable), and {@link #close()} flushes and waits for the writes.
 */
public final class Storage
{
	/** How often changed data files are written, in ticks (3 minutes) */
	public static final long		AUTOSAVE_TICKS			= 3 * 60 * 20L;

	private static final long		CLOSE_TIMEOUT_SECONDS	= 30;

	/**
	 * Earlier versions stored blessed and cursed players in gods.yml as UUID objects, written with a !!java.util.UUID
	 * tag that Bukkit refuses to load. Without the tag they load as plain strings.
	 */
	static final DataFile.Repair	UUID_TAGS				= new DataFile.Repair("removed !!java.util.UUID tags that could not be loaded.", Storage::removeUuidTags);

	private final Logger			logger;
	private final ExecutorService	writer;
	private final GodRepository		gods;
	private final BelieverRepository	believers;
	private final QuestRepository	quests;
	private final HolyLandRepository	holyLands;
	private final MarriageRepository	marriages;
	private final HolyBookRepository	bibles;
	private final GodListRepository	whitelist;
	private final GodListRepository	blacklist;
	private final AltarRepository	altars;
	private final List<DataFile>	files;
	// System.nanoTime() when the data was last (re)loaded, or null; starts the cleanup grace period
	private Long					loadedAt;
	private boolean					loadIncomplete;

	/**
	 * The data files in {@code dataFolder}, written by a background thread of their own.
	 *
	 * @param playerIdByName the UUID of a player the server has seen, by name (any case), or null; upgrades priests and
	 *                       believers that old gods.yml and believers.yml files keep by name
	 */
	public Storage(Path dataFolder, Logger logger, Function<String, UUID> playerIdByName)
	{
		this(dataFolder, logger, Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "Gods-Storage");
			thread.setDaemon(true);
			return thread;
		}), playerIdByName);
	}

	/** Without a server: priests kept by name in an old gods.yml are dropped (and logged) */
	public Storage(Path dataFolder, Logger logger, ExecutorService writer)
	{
		this(dataFolder, logger, writer, name -> null);
	}

	/** @param writer runs the file writes; must run them one at a time, in order */
	public Storage(Path dataFolder, Logger logger, ExecutorService writer, Function<String, UUID> playerIdByName)
	{
		this.logger = logger;
		this.writer = writer;
		this.gods = new GodRepository(new DataFile(dataFolder.resolve("gods.yml"), logger, writer, UUID_TAGS, GodRepository.format(logger, playerIdByName)), logger);
		this.believers = new BelieverRepository(new DataFile(dataFolder.resolve("believers.yml"), logger, writer, null, BelieverRepository.format(logger, playerIdByName)), logger);
		this.quests = new QuestRepository(new DataFile(dataFolder.resolve("quests.yml"), logger, writer, null, QuestRepository.format()), logger);
		this.holyLands = new HolyLandRepository(new DataFile(dataFolder.resolve("holyland.yml"), logger, writer, null, HolyLandRepository.format(logger)), logger);
		this.marriages = new MarriageRepository(new DataFile(dataFolder.resolve("marriages.yml"), logger, writer, null, MarriageRepository.format()), logger);
		this.bibles = new HolyBookRepository(new DataFile(dataFolder.resolve("bibles.yml"), logger, writer, null, HolyBookRepository.format()), logger);
		this.whitelist = new GodListRepository(file(dataFolder, "whitelist.yml", null), logger, "whitelisted");
		this.blacklist = new GodListRepository(file(dataFolder, "blacklist.yml", null), logger, "blacklisted");
		this.altars = new AltarRepository(new DataFile(dataFolder.resolve("altars.yml"), logger, writer, null, AltarRepository.format()), logger);
		this.files = List.of(this.gods.file(), this.believers.file(), this.quests.file(), this.holyLands.file(), this.marriages.file(), this.bibles.file(), this.whitelist.file(), this.blacklist.file(), this.altars.file());
	}

	private DataFile file(Path dataFolder, String name, DataFile.Repair repair)
	{
		return new DataFile(dataFolder.resolve(name), this.logger, this.writer, repair);
	}

	static String removeUuidTags(String text)
	{
		return text.replaceAll("!!java\\.util\\.UUID\\s+", "");
	}

	public GodRepository gods()
	{
		return this.gods;
	}

	public BelieverRepository believers()
	{
		return this.believers;
	}

	public QuestRepository quests()
	{
		return this.quests;
	}

	public HolyLandRepository holyLands()
	{
		return this.holyLands;
	}

	public MarriageRepository marriages()
	{
		return this.marriages;
	}

	public HolyBookRepository bibles()
	{
		return this.bibles;
	}

	public GodListRepository whitelist()
	{
		return this.whitelist;
	}

	public GodListRepository blacklist()
	{
		return this.blacklist;
	}

	/** altars.yml: where the altars are */
	public AltarRepository altars()
	{
		return this.altars;
	}

	public List<DataFile> files()
	{
		return this.files;
	}

	/**
	 * After the data files were (re)loaded: starts the {@link CleanupGuard#GRACE_PERIOD}.
	 *
	 * @param complete false if loading one of them failed: nothing is then removed until a later load works
	 */
	public void dataLoaded(boolean complete)
	{
		this.loadedAt = System.nanoTime();
		this.loadIncomplete = !complete;
	}

	/**
	 * Whether records may be removed because of what other files say (dead gods, inactive priests): not while a file
	 * that is read or changed by that could not be read, not while there are gods but no believers, and not in the
	 * first minutes after a (re)load. See {@link CleanupGuard}.
	 */
	public CleanupGuard.Verdict cleanupVerdict()
	{
		boolean readOnly = this.loadIncomplete || this.gods.isReadOnly() || this.believers.isReadOnly() || this.holyLands.isReadOnly() || this.bibles.isReadOnly();
		Duration sinceLoad = this.loadedAt != null ? Duration.ofNanos(System.nanoTime() - this.loadedAt) : null;
		return CleanupGuard.check(readOnly, this.gods.size(), this.believers.size(), sinceLoad);
	}

	/** Main thread: queues a write for every file with unsaved changes. Does not wait. */
	public void flush()
	{
		for (DataFile file : this.files)
		{
			file.flush();
		}
	}

	/**
	 * Waits until every write queued so far has finished, so the files can be read again (/g reload). Returns false if
	 * that took longer than the timeout.
	 */
	public boolean awaitWrites(long timeout, TimeUnit unit)
	{
		if (this.writer.isShutdown())
		{
			return true;
		}

		try
		{
			Future<?> marker = this.writer.submit(() -> {
			});
			marker.get(timeout, unit);
			return true;
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			return false;
		}
		catch (Exception ex)
		{
			this.logger.warning("Timed out waiting for the data files to be saved.");
			return false;
		}
	}

	/**
	 * /g reload, before the data is loaded again: waits for pending writes, then lets every file decide whether it is
	 * read again (it was edited on disk, or could not be read) or keeps the data in memory, and saves the files that keep
	 * theirs. Returns false if the writes did not finish in time; nothing is changed then.
	 */
	public boolean prepareReload(long timeout, TimeUnit unit)
	{
		if (!awaitWrites(timeout, unit))
		{
			return false;
		}
		for (DataFile file : this.files)
		{
			file.prepareReload();
		}
		flush();
		return true;
	}

	/** After the data was loaded again on /g reload */
	public void finishReload()
	{
		for (DataFile file : this.files)
		{
			file.finishReload();
		}
	}

	/** onDisable: writes every changed file and waits for the writes to finish. Nothing can be saved afterwards. */
	public void close()
	{
		flush();
		this.writer.shutdown();
		try
		{
			if (!this.writer.awaitTermination(CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
			{
				this.logger.severe("The data files were not saved within " + CLOSE_TIMEOUT_SECONDS + " seconds; some changes may be lost.");
			}
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
		}

		for (DataFile file : this.files)
		{
			if (file.isReadOnly())
			{
				this.logger.severe(file.name() + " could not be read and was not saved. Fix it before the next start.");
			}
			else if (file.isEditedOnDisk())
			{
				this.logger.warning(file.name() + " was edited on disk, so it was not saved: your edit is kept, the changes made in the game since are not.");
			}
		}
	}
}
