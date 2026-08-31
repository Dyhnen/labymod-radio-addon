package net.dyhntastic.radio.core.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;

public final class RadioRegMapper {

  private RadioRegMapper() {
  }

  public static RadioStation station(JsonObject json) {
    JsonObject organization = object(json, "organization");
    JsonObject song = object(json, "song");
    JsonObject next = object(song, "next");
    List<String> tags = LautFmMapper.strings(array(json, "tags"));
    String country = firstNonBlank(string(json, "country"), string(organization, "country"));
    return new RadioStation(
        "radioreg:" + longValue(json, "id"),
        string(json, "name"),
        RadioSource.RADIOREG,
        string(json, "url"),
        "https://radioreg.net",
        string(organization, "image"),
        tags,
        tags.isEmpty() ? "" : tags.getFirst(),
        country,
        "",
        new RadioMetadata(
            string(song, "title"),
            string(song, "artist"),
            string(song, "cover"),
            string(next, "title"),
            string(next, "artist")
        )
    );
  }

  public static RadioMetadata history(JsonObject json) {
    JsonObject current = object(json, "current");
    JsonObject next = object(json, "next");
    return new RadioMetadata(
        string(current, "title"),
        string(current, "artist"),
        string(current, "cover"),
        string(next, "title"),
        string(next, "artist")
    );
  }

  static long providerId(String stationId) {
    String value = stationId.startsWith("radioreg:")
        ? stationId.substring("radioreg:".length())
        : stationId;
    return Long.parseLong(value);
  }

  private static JsonObject object(JsonObject parent, String key) {
    JsonElement value = parent == null ? null : parent.get(key);
    return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
  }

  private static JsonArray array(JsonObject parent, String key) {
    JsonElement value = parent == null ? null : parent.get(key);
    return value != null && value.isJsonArray() ? value.getAsJsonArray() : new JsonArray();
  }

  private static String string(JsonObject object, String key) {
    return LautFmMapper.string(object, key);
  }

  private static long longValue(JsonObject object, String key) {
    JsonElement value = object.get(key);
    return value == null || value.isJsonNull() ? 0L : value.getAsLong();
  }

  private static String firstNonBlank(String first, String fallback) {
    return first == null || first.isBlank() ? fallback : first;
  }
}
