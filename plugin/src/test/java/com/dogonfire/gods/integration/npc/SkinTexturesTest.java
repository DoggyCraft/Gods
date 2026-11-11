package com.dogonfire.gods.integration.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class SkinTexturesTest
{
	/** The werewolves theme's skin (taken from the account WerewolfAlpha) */
	private static final String WEREWOLF_ALPHA = "ewogICJ0aW1lc3RhbXAiIDogMTY4NTAzNzE3NjgzNiwKICAicHJvZmlsZUlkIiA6ICJlMGQwNzRiZDY3MjI0N2ZjOTVkM2YyOGUyODk5ZTE1NSIsCiAgInByb2ZpbGVOYW1lIiA6ICJXZXJld29sZkFscGhhIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2NhZTg4YjY5MjA0YWUyYzNlM2NiZTcwOWUzZTYyNTIxZTUzZmEwNWU0ZGFkODkxYzZhZmJkZmI5MDg2OGE3ZTAiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";

	@Test
	void readsTheSkinUrlAndModel()
	{
		assertEquals(new SkinTextures.Texture("http://textures.minecraft.net/texture/cae88b69204ae2c3e3cbe709e3e62521e53fa05e4dad891c6afbdfb90868a7e0", true),
				SkinTextures.decode(WEREWOLF_ALPHA));
	}

	@Test
	void aSkinWithoutModelIsClassic()
	{
		String value = Base64.getEncoder().encodeToString("{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/abc\"}}}".getBytes(StandardCharsets.UTF_8));
		assertEquals(new SkinTextures.Texture("http://textures.minecraft.net/texture/abc", false), SkinTextures.decode(value));
	}

	@Test
	void writesWhatItReads()
	{
		for (SkinTextures.Texture texture : new SkinTextures.Texture[] { new SkinTextures.Texture("http://textures.minecraft.net/texture/abc", false),
				new SkinTextures.Texture("http://textures.minecraft.net/texture/\"quoted\"", true) })
		{
			assertEquals(texture, SkinTextures.decode(SkinTextures.encode(texture)));
		}
	}

	@Test
	void anythingElseIsNoSkin()
	{
		assertNull(SkinTextures.decode(null));
		assertNull(SkinTextures.decode(""));
		assertNull(SkinTextures.decode("not base64 at all!"));
		assertNull(SkinTextures.decode(Base64.getEncoder().encodeToString("not json".getBytes(StandardCharsets.UTF_8))));
		assertNull(SkinTextures.decode(Base64.getEncoder().encodeToString("{\"textures\":{}}".getBytes(StandardCharsets.UTF_8))));
		assertNull(SkinTextures.decode(Base64.getEncoder().encodeToString("{\"textures\":{\"SKIN\":{\"url\":\"\"}}}".getBytes(StandardCharsets.UTF_8))));
	}
}
