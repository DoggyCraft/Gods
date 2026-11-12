package com.dogonfire.gods.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.dogonfire.gods.model.DevotionLevel;
import com.dogonfire.gods.model.GodRank;

class AdminArgsTest
{
	@Test
	void actionsIgnoreCase()
	{
		assertEquals("setrank", AdminArgs.action("SetRank"));
		assertNull(AdminArgs.action("smite"));
		assertEquals("gods.admin.mood", AdminArgs.permission("mood"));
	}

	@Test
	void amountsSetOrChange()
	{
		assertEquals(new AdminArgs.Amount(false, 20), AdminArgs.amount("20"));
		assertEquals(new AdminArgs.Amount(true, 5), AdminArgs.amount("+5"));
		assertEquals(new AdminArgs.Amount(true, -5), AdminArgs.amount("-5"));
		assertEquals(25, AdminArgs.amount("+5").applyTo(20));
		assertEquals(15, AdminArgs.amount("-5").applyTo(20));
		assertEquals(7, AdminArgs.amount("7").applyTo(20));
		assertNull(AdminArgs.amount("five"));
		assertNull(AdminArgs.amount("+"));
		assertNull(AdminArgs.amount("1.5"));
		assertNull(AdminArgs.amount("9999999999"));
	}

	@Test
	void decimalsSetChangeOrSetANegativeValue()
	{
		assertEquals(50.0D, AdminArgs.decimal("50").applyTo(10.0D));
		assertEquals(22.5D, AdminArgs.decimal("+12.5").applyTo(10.0D));
		assertEquals(0.0D, AdminArgs.decimal("-10").applyTo(10.0D));
		assertEquals(-50.0D, AdminArgs.decimal("=-50").applyTo(10.0D));
		assertEquals(5.0D, AdminArgs.decimal("=+5").applyTo(10.0D));
		assertNull(AdminArgs.decimal("much"));
		assertNull(AdminArgs.decimal("--5"));
		assertNull(AdminArgs.decimal("1e9"));
	}

	@Test
	void ranksAndLevelsByKeyOrByTheirThemedName()
	{
		Map<GodRank, String> names = Map.of(GodRank.SPIRIT, "Whisper", GodRank.RECOGNIZED, "Known Flame", GodRank.DESCENDED, "Avatar", GodRank.EXALTED,
				"Exalted", GodRank.GREATER, "Sun Lord");
		assertEquals(GodRank.EXALTED, AdminArgs.named("exalted", GodRank.values(), names::get));
		assertEquals(GodRank.GREATER, AdminArgs.named("GREATER", GodRank.values(), names::get));
		assertEquals(GodRank.RECOGNIZED, AdminArgs.named("known flame", GodRank.values(), names::get));
		assertEquals(GodRank.RECOGNIZED, AdminArgs.named("Known_Flame", GodRank.values(), names::get));
		assertEquals(GodRank.DESCENDED, AdminArgs.named("avatar", GodRank.values(), names::get));
		assertNull(AdminArgs.named("emperor", GodRank.values(), names::get));
		assertNull(AdminArgs.named(" ", GodRank.values(), names::get));
		assertEquals(DevotionLevel.SAINT, AdminArgs.named("saint", DevotionLevel.values(), level -> null));

		assertEquals(List.of("spirit", "recognized", "descended", "exalted", "greater", "whisper", "avatar"), AdminArgs.completions(GodRank.values(), names::get));
	}
}
