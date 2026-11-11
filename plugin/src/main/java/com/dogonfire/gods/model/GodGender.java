package com.dogonfire.gods.model;

/**
 * A god's gender, chosen from the altar it was created at (or by a priest with /g gender). Stored as {@code Gender} in
 * gods.yml by name: {@code None} (gods from before genders could be chosen), {@code Male}, {@code Female} or
 * {@code NonBinary}.
 */
public enum GodGender
{
	None, Male, Female, NonBinary;
}
