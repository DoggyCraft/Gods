package com.dogonfire.gods.model;

/**
 * What a holy artifact does when it is used. Stored by name in the artifact's {@code gods:holy_power} tag; artifacts
 * from before the tag describe it in their lore ({@code HolyPowerRules.describe}).
 */
public enum HolyPower
{
	KNOWLEDGE, CALLMOON, CALLSUN, HEALING, TAME, LIGHTNING, LIGHTNING_STORM, TELEPORT, FREEZE, ICE, FIREBALL, EARTHQUAKE, NATURE, FIREWORK, DRUNK;
}
