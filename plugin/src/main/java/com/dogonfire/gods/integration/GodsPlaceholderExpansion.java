package com.dogonfire.gods.integration;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.managers.ChatManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * The %gods_...% placeholders of PlaceholderAPI. Registered by {@link com.dogonfire.gods.Gods} only when
 * PlaceholderAPI is installed (this class references its classes), and unregistered on disable.
 */
public class GodsPlaceholderExpansion extends PlaceholderExpansion
{
	private final GodsContext ctx;

	public GodsPlaceholderExpansion(GodsContext ctx)
	{
		this.ctx = ctx;
	}

	/** Registered by Gods itself, so it stays when PlaceholderAPI reloads */
	@Override
	public boolean persist()
	{
		return true;
	}

	@Override
	public boolean canRegister()
	{
		return true;
	}

	@Override
	public String getAuthor()
	{
		return String.join(", ", this.ctx.plugin().getDescription().getAuthors());
	}

	/** %gods_<value>% */
	@Override
	public String getIdentifier()
	{
		return "gods";
	}

	@Override
	public String getVersion()
	{
		return this.ctx.plugin().getDescription().getVersion();
	}

	@Override
	public String onPlaceholderRequest(Player player, String identifier)
	{
		if (player == null)
		{
			return "";
		}

		// Chat plugins resolve placeholders on the async chat thread, where the YAML data must not be
		// read; ChatManager keeps a snapshot for that
		String god;
		int prayerPower;
		ChatColor godColor;
		if (Bukkit.isPrimaryThread())
		{
			god = this.ctx.believerService().getGodForBeliever(player.getUniqueId());
			prayerPower = this.ctx.believerService().getPrayerPower(player.getUniqueId());
			godColor = (god != null) ? this.ctx.godManager().getColorForGod(god) : ChatColor.WHITE;
		}
		else
		{
			ChatManager.ChatInfo info = this.ctx.chatManager().getChatInfo(player.getUniqueId());
			god = info.godName;
			prayerPower = info.prayerPower;
			godColor = info.godColor;
		}

		return switch (identifier)
		{
			// %gods_name_space%
			case "name_space" -> (god == null) ? "" : god + " ";
			// %gods_name%
			case "name" -> (god == null) ? "" : god;
			// %gods_prayerpower%
			case "prayerpower" -> String.valueOf(prayerPower);
			// %gods_color%
			case "color" -> "&" + ((godColor != null) ? godColor : ChatColor.WHITE).getChar();
			// An invalid placeholder (f.e. %gods_cakenomnom%)
			default -> null;
		};
	}
}
