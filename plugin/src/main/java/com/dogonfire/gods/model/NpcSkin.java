package com.dogonfire.gods.model;

/**
 * The skin a god's NPC wears: a player's name (resolved by the server, see {@code
 * integration/npc/SkinResolver}), a texture (the {@code textures} property of a player profile: its
 * base64 {@code value} and Mojang's {@code signature}), or both (a name already resolved). A skin
 * set with /g godskin keeps the name it came from and the texture it resolved to, so the god keeps
 * it if the player changes their skin or Mojang can't be reached.
 *
 * @param name a player name, or null
 * @param value the texture value (base64 JSON), or null
 * @param signature Mojang's signature of the value, or null (unsigned: fine for mannequins, not for
 *     player NPCs)
 */
public record NpcSkin(String name, String value, String signature) {
  public NpcSkin {
    name = blankToNull(name);
    value = blankToNull(value);
    signature = blankToNull(signature);
  }

  /** A skin by player name, still to be resolved */
  public static NpcSkin named(String name) {
    return new NpcSkin(name, null, null);
  }

  /** Whether it has a texture (else only a name) */
  public boolean hasTexture() {
    return this.value != null;
  }

  /** Whether the texture is signed by Mojang */
  public boolean isSigned() {
    return this.value != null && this.signature != null;
  }

  /** Whether it says anything (a name or a texture) */
  public boolean isEmpty() {
    return this.name == null && this.value == null;
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.trim();
  }
}
