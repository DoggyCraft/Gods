package com.dogonfire.gods.api;

/**
 * How a god feels, from its mood value (-100 to 100): below -70 it is {@link #ANGRY}, below -20 {@link #DISPLEASED},
 * below 20 {@link #NEUTRAL}, below 70 {@link #PLEASED} and else {@link #EXALTED}. Angry gods curse their believers;
 * pleased ones bless them.
 */
public enum GodMood
{
	EXALTED, PLEASED, NEUTRAL, DISPLEASED, ANGRY
}
