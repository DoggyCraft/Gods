package com.dogonfire.gods.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Writes files through a temporary file that is flushed to the disk and then moved into place, so a crash or power
 * loss mid-write never leaves a half-written (or empty) file.
 */
public final class AtomicFiles
{
	private AtomicFiles()
	{
	}

	public static void write(Path target, String content) throws IOException
	{
		Files.createDirectories(target.toAbsolutePath().getParent());
		Path temp = target.resolveSibling(target.getFileName() + ".tmp");
		ByteBuffer bytes = ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));
		try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))
		{
			while (bytes.hasRemaining())
			{
				channel.write(bytes);
			}
			// On the disk before the move, or a crash right after it could leave the new name pointing at nothing
			channel.force(true);
		}
		try
		{
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		catch (AtomicMoveNotSupportedException e)
		{
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
