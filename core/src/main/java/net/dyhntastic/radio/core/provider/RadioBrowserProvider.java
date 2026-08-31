package net.dyhntastic.radio.core.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;
import net.dyhntastic.radio.api.StationPage;
import net.dyhntastic.radio.api.StationProvider;
import net.dyhntastic.radio.core.net.HttpJsonClient;
import net.dyhntastic.radio.core.net.IcyMetadataClient;
import net.dyhntastic.radio.core.util.TtlCache;

/** International station catalog backed by the community Radio Browser API. */
public final class RadioBrowserProvider implements StationProvider {

  static final String API = "https://all.api.radio-browser.info/json";
  private final HttpJsonClient http;
  private final IcyMetadataClient icy;
  private final TtlCache<String, Object> cache;

  public RadioBrowserProvider(
      HttpJsonClient http,
      IcyMetadataClient icy,
      TtlCache<String, Object> cache
  ) {
    this.http = http;
    this.icy = icy;
    this.cache = cache;
  }

  @Override
  public RadioSource source() {
    return RadioSource.RADIO_BROWSER;
  }

  @Override
  public CompletableFuture<StationPage> search(String query, int offset, int limit) {
    String name = query == null ? "" : query.trim();
    if (name.isBlank()) {
      return this.discover(offset, limit);
    }
    return this.loadStations(name, offset, limit);
  }

  @Override
  public CompletableFuture<StationPage> discover(int offset, int limit) {
    return this.loadStations("", offset, limit);
  }

  @Override
  public CompletableFuture<RadioStation> details(String stationId) {
    String uuid = providerId(stationId);
    return cached("browser:details:" + uuid, Duration.ofMinutes(10), () ->
        this.http.get(API + "/stations/byuuid/" + encode(uuid))
            .thenApply(json -> {
              List<RadioStation> stations = parseStations(json);
              if (stations.isEmpty()) {
                throw new IllegalArgumentException("Unknown Radio Browser station " + uuid);
              }
              return stations.getFirst();
            })
    );
  }

  @Override
  public CompletableFuture<RadioMetadata> metadata(RadioStation station) {
    return cached("browser:metadata:" + station.id(), Duration.ofSeconds(4), () ->
        this.icy.fetch(station.streamUrl())
    );
  }

  private CompletableFuture<StationPage> loadStations(String name, int offset, int limit) {
    int safeOffset = Math.max(0, offset);
    int safeLimit = Math.max(1, Math.min(100, limit));
    StringBuilder url = new StringBuilder(API)
        .append("/stations/search?hidebroken=true&codec=MP3&order=clickcount&reverse=true")
        .append("&offset=").append(safeOffset)
        .append("&limit=").append(safeLimit);
    if (!name.isBlank()) {
      url.append("&name=").append(encode(name)).append("&nameExact=false");
    }
    String key = "browser:list:" + name.toLowerCase() + ':' + safeOffset + ':' + safeLimit;
    return cached(key, Duration.ofMinutes(2), () -> this.http.get(url.toString())
        .thenApply(json -> {
          List<RadioStation> stations = parseStations(json);
          int estimatedTotal = safeOffset + stations.size() + (stations.size() == safeLimit ? 1 : 0);
          return new StationPage(stations, safeOffset, safeLimit, estimatedTotal);
        }));
  }

  static List<RadioStation> parseStations(JsonElement json) {
    if (!json.isJsonArray()) {
      throw new IllegalStateException("Unexpected Radio Browser response");
    }
    List<RadioStation> stations = new ArrayList<>();
    for (JsonElement element : json.getAsJsonArray()) {
      if (!element.isJsonObject()) {
        continue;
      }
      JsonObject object = element.getAsJsonObject();
      String uuid = string(object, "stationuuid");
      String name = string(object, "name").strip();
      String stream = string(object, "url_resolved");
      if (stream.isBlank()) {
        stream = string(object, "url");
      }
      String codec = string(object, "codec");
      if (uuid.isBlank() || name.isBlank() || stream.isBlank() || !"MP3".equalsIgnoreCase(codec)) {
        continue;
      }
      List<String> tags = splitTags(string(object, "tags"));
      stations.add(new RadioStation(
          "radio-browser:" + uuid,
          name,
          RadioSource.RADIO_BROWSER,
          stream,
          string(object, "homepage"),
          string(object, "favicon"),
          tags,
          tags.isEmpty() ? "" : tags.getFirst(),
          string(object, "countrycode"),
          string(object, "language"),
          RadioMetadata.EMPTY
      ));
    }
    return List.copyOf(stations);
  }

  private static List<String> splitTags(String value) {
    if (value.isBlank()) {
      return List.of();
    }
    Set<String> unique = new LinkedHashSet<>();
    for (String valuePart : value.split(",")) {
      String tag = valuePart.strip();
      if (!tag.isBlank()) {
        unique.add(tag);
      }
      if (unique.size() == 12) {
        break;
      }
    }
    return List.copyOf(unique);
  }

  private static String providerId(String id) {
    return id != null && id.startsWith("radio-browser:") ? id.substring(14) : String.valueOf(id);
  }

  private static String string(JsonObject object, String key) {
    JsonElement value = object.get(key);
    return value == null || value.isJsonNull() ? "" : value.getAsString();
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
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
}
