package com.dogonfire.gods.integration;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.dogonfire.gods.service.AltarClaims;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

/**
 * GriefPrevention claims. Only created when GriefPrevention is installed and enabled (see
 * {@link com.dogonfire.gods.GodsContext}), since this class references GriefPrevention's classes.
 */
public final class GriefPreventionIntegration
{
	/** Where a location is, as far as building an altar there is concerned ({@link AltarClaims}) */
	public AltarClaims.Claim claimAt(Player player, Location location)
	{
		GriefPrevention griefPrevention = GriefPrevention.instance;
		if (griefPrevention == null || location.getWorld() == null || !griefPrevention.claimsEnabledForWorld(location.getWorld()))
		{
			return AltarClaims.Claim.NO_CLAIMS_IN_WORLD;
		}
		Claim claim = griefPrevention.dataStore.getClaimAt(location, true, null);
		if (claim == null)
		{
			return AltarClaims.Claim.NONE;
		}
		// Null means allowed: the owner, a player with build trust, or an admin ignoring claims
		return claim.checkPermission(player, ClaimPermission.Build, null) == null ? AltarClaims.Claim.TRUSTED : AltarClaims.Claim.UNTRUSTED;
	}
}
