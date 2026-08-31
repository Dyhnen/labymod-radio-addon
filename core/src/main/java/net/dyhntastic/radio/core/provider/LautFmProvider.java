package net.dyhntastic.radio.core.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;
import net.dyhntastic.radio.api.StationPage;
import net.dyhntastic.radio.api.StationProvider;
import net.dyhntastic.radio.core.net.HttpJsonClient;
import net.dyhntastic.radio.core.util.TtlCache;

public final class LautFmProvider implements StationProvider {

  static final String API = "https://api.laut.fm";
  private final HttpJsonClient http;
  private final TtlCache<String, Object> cache;

  public LautFmProvider(HttpJsonClient http, TtlCache<String, Object> cache) {
    this.http = http;
    this.cache = cache;
  }

  @Override
  public RadioSource source() {
    return RadioSource.LAUTFM;
  }

  @Override
  public CompletableFuture<StationPage> search(String query, int offset, int limit) {
    String normalized = query == null ? "" : query.trim();
    if (normalized.isEmpty()) {
      return this.discover(offset, limit);
    }
    String url = API + "/search/stations?query=" + encode(normalized)
        + "&offset=" + Math.max(0, offset) + "&limit=" + Math.max(1, limit);
    String key = "laut:search:" + normalized.toLowerCase() + ':' + offset + ':' + limit;
    return cached(key, Duration.ofMinutes(2), () -> this.http.get(url)
        .thenApply(json -> parseSearch(json, offset, limit)));
  }

  @Override
  public CompletableFuture<StationPage> discover(int offset, int limit) {
    int safeOffset = Math.max(0, offset);
    int safeLimit = Math.max(1, limit);
    String key = "laut:discover:" + safeOffset + ':' + safeLimit;
    String url = API + "/stations?offset=" + safeOffset + "&limit=" + safeLimit;
    return cached(key, Duration.ofMinutes(5), () -> this.http.get(url)
        .thenApply(json -> parseDiscovery(json, safeOffset, safeLimit)));
  }

  @Override
  public CompletableFuture<RadioStation> details(String stationId) {
    String id = LautFmMapper.providerId(stationId);
    String key = "laut:details:" + id;
    return cached(key, Duration.ofMinutes(10), () -> this.http.get(API + "/station/" + encode(id))
        .thenApply(json -> LautFmMapper.station(json.getAsJsonObject(), null)));
  }

  @Override
  public CompletableFuture<RadioMetadata> metadata(RadioStation station) {
    String id = LautFmMapper.providerId(station.id());
    String key = "laut:metadata:" + id;
    return cached(key, Duration.ofSeconds(4), () -> {
      CompletableFuture<JsonElement> current = this.http.get(API + "/station/" + encode(id) + "/current_song");
      CompletableFuture<JsonElement> next = this.http.get(API + "/station/" + encode(id) + "/next_artists")
          .exceptionally(error -> new JsonArray());
      return current.thenCombine(next, (song, artists) -> LautFmMapper.metadata(
          song.getAsJsonObject(),
          artists.isJsonArray() ? artists.getAsJsonArray() : new JsonArray()
      ));
    });
  }

  @Override
  public CompletableFuture<List<String>> genres() {
    return cached("laut:genres", Duration.ofHours(6), () -> this.http.get(API + "/genres")
        .thenApply(json -> {
          List<String> genres = new ArrayList<>();
          for (JsonElement element : json.getAsJsonArray()) {
            genres.add(LautFmMapper.string(element.getAsJsonObject(), "name"));
          }
          return genres;
        }));
  }

  @SuppressWarnings("unchecked")
  private <T> CompletableFuture<T> cached(
      String key,
      Duration ttl,
      java.util.function.Supplier<CompletableFuture<T>> loader
  ) {
    return (CompletableFuture<T>) (CompletableFuture<?>) this.cache.getOrLoad(
        key,
        ttl,
        () -> (CompletableFuture<Object>) (CompletableFuture<?>) loader.get()
    );
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static int intValue(JsonObject object, String key, int fallback) {
    JsonElement value = object.get(key);
    return value == null || value.isJsonNull() ? fallback : value.getAsInt();
  }

  static StationPage parseDiscovery(JsonElement json, int offset, int limit) {
    JsonArray items;
    int total;
    if (json.isJsonObject()) {
      JsonObject root = json.getAsJsonObject();
      items = root.getAsJsonArray("items");
      total = intValue(root, "total", items == null ? 0 : items.size());
    } else if (json.isJsonArray()) {
      items = json.getAsJsonArray();
      total = offset + items.size();
    } else {
      throw new IllegalStateException("Unexpected laut.fm station response");
    }
    if (items == null) {
      items = new JsonArray();
    }
    List<RadioStation> stations = new ArrayList<>();
    for (JsonElement element : items) {
      if (element.isJsonObject()) {
        stations.add(LautFmMapper.station(element.getAsJsonObject(), null));
      }
    }
    return new StationPage(stations, offset, limit, total);
  }

  static StationPage parseSearch(JsonElement json, int offset, int limit) {
    if (!json.isJsonObject()) {
      throw new IllegalStateException("Unexpected laut.fm search response");
    }
    JsonObject root = json.getAsJsonObject();
    Map<String, RadioStation> unique = new LinkedHashMap<>();
    JsonElement resultsElement = root.get("results");
    if (resultsElement != null && resultsElement.isJsonArray()) {
      for (JsonElement resultElement : resultsElement.getAsJsonArray()) {
        if (!resultElement.isJsonObject()) {
          continue;
        }
        JsonElement itemsElement = resultElement.getAsJsonObject().get("items");
        if (itemsElement == null || !itemsElement.isJsonArray()) {
          continue;
        }
        for (JsonElement itemElement : itemsElement.getAsJsonArray()) {
          if (!itemElement.isJsonObject()) {
            continue;
          }
          JsonObject item = itemElement.getAsJsonObject();
          JsonElement stationElement = item.get("station");
          if (stationElement == null || !stationElement.isJsonObject()) {
            continue;
          }
          JsonObject currentSong = item.has("current_song") && item.get("current_song").isJsonObject()
              ? item.getAsJsonObject("current_song")
              : null;
          RadioStation station = LautFmMapper.station(stationElement.getAsJsonObject(), currentSong);
          if (!station.name().isBlank() && !station.streamUrl().isBlank()) {
            unique.putIfAbsent(station.id(), station);
          }
        }
      }
    }
    return new StationPage(
        new ArrayList<>(unique.values()),
        intValue(root, "offset", offset),
        intValue(root, "limit", limit),
        intValue(root, "total", unique.size())
    );
  }
}
