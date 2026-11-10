package com.dogonfire.gods;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.dogonfire.gods.managers.GodManager;

public class GodsTest
{
	@Test
	@DisplayName("Static instance must always return a non-null value")
	public void testGodManagerInstance()
	{
		GodManager manager = GodManager.instance();

		assertNotNull(manager);
	}
}
