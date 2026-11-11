package com.dogonfire.gods.command;

/** Thrown when the arguments don't fit; the router answers with the command's usage (command.usage) */
public final class UsageException extends Exception
{
	public UsageException()
	{
		super(null, null, false, false);
	}
}
