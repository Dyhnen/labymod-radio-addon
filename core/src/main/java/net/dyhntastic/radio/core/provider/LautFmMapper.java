package net.dyhntastic.radio.core.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;

public final class LautFmMapper {

  private LautFmMapper() {
  }

  public static RadioStation station(JsonObject json, JsonObject currentSong) {
    String id = string(json, "name");
    List<String> genres = strings(json.getAsJsonArray("genres"));
    JsonObject images = object(json, "images");
    JsonObject thirdParties = object(json, "third_parties");
    JsonObject website = object(thirdParties, "website");
    String homepage = string(json, "page_url");
    if (website.has("url")) {
      homepage = string(website, "url");
    }
    return new RadioStation(
        "laut:" + id,
        firstNonBlank(string(json, "display_name"), id),
        RadioSource.LAUTFM,
        string(json, "stream_url"),
        homepage,
        firstNonBlank(string(images, "station_120x120"), string(images, "station")),
        genres,
        genres.isEmpty() ? "" : genres.getFirst(),
        "",
        "",
        metadata(currentSong, null)
    );
  }

  public static RadioMetadata metadata(JsonObject song, JsonArray nextArtists) {
    if (song == null) {
      song = new JsonObject();
    }
    JsonObject artist = object(song, "artist");
    String nextArtist = "";
    if (nextArtists != null && !nextArtists.isEmpty() && nextArtists.get(0).isJsonObject()) {
      nextArtist = string(object(nextArtists.get(0).getAsJsonObject(), "artist"), "name");
    }
    return new RadioMetadata(
        string(song, "title"),
        string(artist, "name"),
        "",
        "",
        nextArtist
    );
  }

  static String providerId(String stationId) {
    return stationId.startsWith("laut:") ? stationId.substring("laut:".length()) : stationId;
  }

  static JsonObject object(JsonObject parent, String key) {
    JsonElement value = parent == null ? null : parent.get(key);
    return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
  }

  static String string(JsonObject object, String key) {
    JsonElement value = object == null ? null : object.get(key);
    return value == null || value.isJsonNull() ? "" : value.getAsString();
  }

  static List<String> strings(JsonArray array) {
    if (array == null) {
      return List.of();
    }
    List<String> values = new ArrayList<>();
    for (JsonElement element : array) {
      if (element.isJsonPrimitive()) {
        values.add(element.getAsString());
      }
    }
    return values;
  }

  private static String firstNonBlank(String first, String fallback) {
    return first == null || first.isBlank() ? fallback : first;
  }
}
