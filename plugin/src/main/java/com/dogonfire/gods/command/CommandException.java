package com.dogonfire.gods.command;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/** Stops a command and tells the sender why, with a message from messages.yml */
public final class CommandException extends Exception
{
	private final String				messageKey;
	private final transient TagResolver[]	tags;

	public CommandException(String messageKey, TagResolver... tags)
	{
		super(messageKey, null, false, false);
		this.messageKey = messageKey;
		this.tags = tags;
	}

	public String messageKey()
	{
		return this.messageKey;
	}

	public TagResolver[] tags()
	{
		return this.tags;
	}
}
