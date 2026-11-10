package com.dogonfire.gods.service;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.model.ContestedLand;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.storage.GodRepository;

/**
 * Relations between gods: alliances and wars (priests toggle them with /g alliance and /g war), and the holy land a
 * god attacks. {@link HolyLandService} owns the attack itself (on the land: who attacks, kills, when it ends); this
 * keeps the attacker's side in gods.yml and drops it once the land no longer has this god as attacker. The rules are
 * in {@link RelationRules}. Main thread only.
 */
public class RelationService
{
	private final GodsContext		ctx;
	private final GodRepository		repository;

	public RelationService(GodsContext ctx, GodRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
	}

	private God god(String godName)
	{
		return this.repository.find(godName);
	}

	// ---------------------------------------------------------------------
	// Alliances and wars

	/** The god's allies (a copy) */
	public List<String> getAllianceRelations(String godName)
	{
		God god = god(godName);
		return god != null ? new ArrayList<String>(god.allies()) : new ArrayList<String>();
	}

	/** The gods it is at war with (a copy) */
	public List<String> getWarRelations(String godName)
	{
		God god = god(godName);
		return god != null ? new ArrayList<String>(god.enemies()) : new ArrayList<String>();
	}

	public boolean hasAllianceRelation(String godName, String otherGodName)
	{
		return getAllianceRelations(godName).contains(otherGodName);
	}

	public boolean hasWarRelation(String godName, String otherGodName)
	{
		return getWarRelations(godName).contains(otherGodName);
	}

	/**
	 * Makes the two gods allies (ending any war between them), or ends their alliance if they are allies.
	 *
	 * @return true if they are allies now
	 */
	public boolean toggleAllianceRelationForGod(String godName, String allyGodName)
	{
		if (!hasAllianceRelation(godName, allyGodName))
		{
			RelationRules.makeAllies(this.repository.forChange(godName), this.repository.forChange(allyGodName));
			this.repository.markDirty();

			return true;
		}

		RelationRules.end(god(godName), godName, god(allyGodName), allyGodName, true);
		this.repository.save();

		return false;
	}

	/**
	 * Puts the two gods at war (ending any alliance between them), or ends their war if they are at war.
	 *
	 * @return true if they are at war now
	 */
	public boolean toggleWarRelationForGod(String godName, String enemyGodName)
	{
		if (!hasWarRelation(godName, enemyGodName))
		{
			RelationRules.makeEnemies(this.repository.forChange(godName), this.repository.forChange(enemyGodName));
			this.repository.markDirty();

			return true;
		}

		RelationRules.end(god(godName), godName, god(enemyGodName), enemyGodName, false);
		this.repository.save();

		return false;
	}

	/** A god is removed: no other god is its ally or enemy any more */
	public void removeRelationsWith(String godName)
	{
		for (God other : this.repository.all())
		{
			RelationRules.forget(other, godName);
		}
	}

	// ---------------------------------------------------------------------
	// The holy land a god attacks

	/** A priest started an attack on the holy land at {@code contestedLand} */
	public void setContestedHolyLandForGod(String godName, Location contestedLand)
	{
		God god = this.repository.forChange(godName);
		if (god != null)
		{
			god.setContestedLand(new ContestedLand(contestedLand.getWorld().getName(), contestedLand.getBlockX(), contestedLand.getBlockY(), contestedLand.getBlockZ(),
					ctx.holyLandService().hashLocation(contestedLand)));
		}

		ctx.holyLandService().setContestedLand(contestedLand, godName);

		this.repository.markDirty();
	}

	public void clearContestedHolyLandForGod(String godName)
	{
		God god = god(godName);
		if (god != null)
		{
			GodRepository.clearContestedLand(god);
		}

		this.repository.markDirty();
	}

	/** Where the attack was started (the world is null if it isn't loaded, or there is no attack) */
	public Location getContestedHolyLandAttackLocationForGod(String godName)
	{
		God god = god(godName);
		ContestedLand land = god != null ? god.contestedLand() : null;
		if (land == null)
		{
			return new Location(null, 0, 0, 0);
		}

		return new Location(ctx.plugin().getServer().getWorld(land.world()), land.x(), land.y(), land.z());
	}

	/**
	 * The region hash of the holy land godName is attacking, or null. An attack whose land no longer has
	 * godName as attacker (it was settled, abandoned or taken over, or comes from an old data file) is cleared.
	 */
	public Long getContestedHolyLandForGod(String godName)
	{
		God god = god(godName);
		if (god == null || god.contestedLand() == null)
		{
			return null;
		}
		Location location = getContestedHolyLandAttackLocationForGod(godName);
		if (!ctx.holyLandService().isAttackingHolyLand(godName, location))
		{
			clearContestedHolyLandForGod(godName);
			return null;
		}
		return ctx.holyLandService().hashLocation(location);
	}
}
