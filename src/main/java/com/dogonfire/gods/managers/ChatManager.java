package com.dogonfire.gods.managers;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;

public class ChatManager
{
	private static ChatManager instance;

	public static ChatManager instance()
	{
		if (instance == null)
			instance = new ChatManager();
		return instance;
	}

	private String				chatTag				= "[GOD]";

	private String				playerChatFormat	= "";
	private FileConfiguration	chatConfig;
	private File				chatConfigFile;

	private ChatManager()
	{
	}

	public String addColor(String string)
	{
		return ChatColor.translateAlternateColorCodes('&', string);
	}

	public String formatChat(Player player, String godName, String message)
	{
		if (godName != null)
		{
			this.playerChatFormat = message.replace(this.chatTag, godName + " ");
		}
		else
		{
			this.playerChatFormat = message.replace(this.chatTag, "");
		}
		return this.playerChatFormat;
	}

	public void load()
	{

	}

	public void save()
	{
		if ((this.chatConfig == null) || (this.chatConfigFile == null))
		{
			return;
		}
		try
		{
			this.chatConfig.save(this.chatConfigFile);
		}
		catch (Exception ex)
		{
			Gods.instance().log("Could not save config to " + this.chatConfigFile.getName() + ": " + ex.getMessage());
		}
	}
}