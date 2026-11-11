package com.dogonfire.gods.config;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;

import com.dogonfire.gods.model.GodType;

/**
 * The god types' themes (themes.yml), read by {@link ThemesLoader} on enable and /g reload and never changed
 * afterwards. Read them through {@code ctx.themes()} every time, like the settings.
 *
 * @param defaults the {@code default} theme: what a god without a type uses
 * @param types    the theme of each god type (every god type has one; gaps were filled from {@code default})
 * @param altars   the altar blocks in use: for a god type that config.yml's {@code altars.block-types} lists, that
 *                 list as it is; for the others the theme's {@link Theme#altarBlocks()}
 */
public record Themes(Theme defaults, Map<GodType, Theme> types, Settings.Altars altars)
{
	/** The theme of a god type; the default theme for null (a god without a type) */
	public Theme of(GodType type)
	{
		return type == null ? this.defaults : this.types.getOrDefault(type, this.defaults);
	}

	/**
	 * The altar blocks: config.yml's list for each god type it lists (as it is, so an admin's list, even an empty one,
	 * wins), the theme's for the others.
	 */
	public static Settings.Altars combineAltars(Settings.Altars configured, Map<GodType, Theme> types)
	{
		Map<GodType, List<Material>> blocks = new EnumMap<>(GodType.class);
		for (GodType type : GodType.values())
		{
			if (configured.blockTypes().containsKey(type))
			{
				blocks.put(type, configured.blockTypes().get(type));
			}
			else if (types.containsKey(type))
			{
				blocks.put(type, types.get(type).altarBlocks());
			}
		}
		return Settings.Altars.of(Collections.unmodifiableMap(blocks), configured.requireClaim());
	}
}
