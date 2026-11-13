package com.dogonfire.gods.integration.npc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Reads and writes the value of a player profile's {@code textures} property (pure, tested): base64
 * of {@code {"textures":{"SKIN":{"url":"...","metadata":{"model":"slim"}}}}}. Spigot's profile API
 * only takes the skin URL and model, so the built-in NPC reads them from a texture value there; a
 * player's skin resolved on Spigot comes back as a URL and model and is written as an (unsigned)
 * value.
 */
public final class SkinTextures {
  private SkinTextures() {}

  /** A skin: its texture URL and whether it uses the slim (Alex) arms */
  public record Texture(String url, boolean slim) {}

  /**
   * The skin in a texture value, or null if the value isn't one (not base64, not JSON, no skin URL)
   */
  public static Texture decode(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      String json =
          new String(
              Base64.getDecoder().decode(value.replaceAll("\\s", "")), StandardCharsets.UTF_8);
      JsonObject skin =
          JsonParser.parseString(json)
              .getAsJsonObject()
              .getAsJsonObject("textures")
              .getAsJsonObject("SKIN");
      JsonElement url = skin.get("url");
      if (url == null || !url.isJsonPrimitive() || url.getAsString().isBlank()) {
        return null;
      }
      JsonObject metadata =
          skin.has("metadata") && skin.get("metadata").isJsonObject()
              ? skin.getAsJsonObject("metadata")
              : null;
      boolean slim =
          metadata != null
              && metadata.has("model")
              && "slim".equals(metadata.get("model").getAsString());
      return new Texture(url.getAsString(), slim);
    } catch (RuntimeException e) {
      // IllegalArgumentException (base64), JsonParseException, IllegalStateException,
      // ClassCastException, NPE
      return null;
    }
  }

  /** An unsigned texture value for the skin */
  public static String encode(Texture texture) {
    JsonObject skin = new JsonObject();
    skin.addProperty("url", texture.url());
    if (texture.slim()) {
      JsonObject metadata = new JsonObject();
      metadata.addProperty("model", "slim");
      skin.add("metadata", metadata);
    }
    JsonObject textures = new JsonObject();
    textures.add("SKIN", skin);
    JsonObject root = new JsonObject();
    root.add("textures", textures);
    return Base64.getEncoder().encodeToString(root.toString().getBytes(StandardCharsets.UTF_8));
  }
}
