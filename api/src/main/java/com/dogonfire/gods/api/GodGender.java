package com.dogonfire.gods.api;

/**
 * A god's gender, chosen by the altar it was created at or by a priest. {@link #NONE} gods speak in a voice picked by
 * chance; {@link #NON_BINARY} gods ("Deity") speak with both the male and the female voice of their type.
 */
public enum GodGender
{
	NONE, MALE, FEMALE,
	/** Neither: a "Deity", speaking with the male and the female voice of its type, picked anew for every line */
	NON_BINARY
}
