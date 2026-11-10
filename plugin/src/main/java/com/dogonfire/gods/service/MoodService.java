package com.dogonfire.gods.service;

import com.dogonfire.gods.GodsContext;
import com.dogonfire.gods.model.God;
import com.dogonfire.gods.model.GodMood;
import com.dogonfire.gods.storage.GodRepository;

/**
 * Gods' moods: what pleases and angers them, and the slow falloff while their believers are online. The mood decides
 * what a god does on its turn (see {@code GodUpdateTask}). The rules are in {@link MoodRules}. Main thread only.
 */
public class MoodService
{
	private final GodsContext		ctx;
	private final GodRepository		repository;

	public MoodService(GodsContext ctx, GodRepository repository)
	{
		this.ctx = ctx;
		this.repository = repository;
	}

	/** Adds to (or with a negative value, takes from) the god's mood, within -100 to 100 */
	public void addMoodForGod(String godName, float mood)
	{
		God god = this.repository.forChange(godName);
		if (god == null)
		{
			return;
		}

		god.setMood(MoodRules.add(god.mood(), mood));

		this.repository.markDirty();
	}

	/** The mood value, -100 to 100 */
	public float getExactMoodForGod(String godName)
	{
		God god = this.repository.find(godName);
		return god != null ? god.mood() : 0.0F;
	}

	public GodMood getMoodForGod(String godName)
	{
		return MoodRules.moodFor(getExactMoodForGod(godName));
	}

	/** How much a good deed pleases the god */
	public float getPleasedModifierForGod(String godName)
	{
		return MoodRules.pleasedModifier(ctx.godService().getSeedForGod(godName));
	}

	/** How much a bad deed angers the god */
	public float getAngryModifierForGod(String godName)
	{
		return MoodRules.ANGRY_MODIFIER;
	}

	public float getFalloffModifierForGod(String godName)
	{
		float falloffValue = MoodRules.falloff(ctx.godService().getSeedForGod(godName), ctx.settings().gods().moodFalloff(), ctx.believerService().getOnlineBelieversForGod(godName).size(),
				System.currentTimeMillis());

		ctx.plugin().logDebug(godName + " mood falloff is " + (double) falloffValue);

		return falloffValue;
	}

	/** The god's turn: its mood falls off while believers are online */
	public void applyFalloff(String godName)
	{
		if (ctx.believerService().getOnlineBelieversForGod(godName).size() == 0)
		{
			return;
		}
		addMoodForGod(godName, getFalloffModifierForGod(godName));
	}
}
