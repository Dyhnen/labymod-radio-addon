package net.dyhntastic.radio.core.provider;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;
import net.dyhntastic.radio.api.StationPage;
import net.dyhntastic.radio.api.StationProvider;
import net.dyhntastic.radio.core.net.HttpJsonClient;
import net.dyhntastic.radio.core.audio.StreamFormatDetector;
import net.dyhntastic.radio.core.util.TtlCache;

public final class RadioRegProvider implements StationProvider {

  static final String API = "https://api.radioreg.net";
  private final HttpJsonClient http;
  private final TtlCache<String, Object> cache;

  public RadioRegProvider(HttpJsonClient http, TtlCache<String, Object> cache) {
    this.http = http;
    this.cache = cache;
  }

  @Override
  public RadioSource source() {
    return RadioSource.RADIOREG;
  }

  @Override
  public CompletableFuture<StationPage> search(String query, int offset, int limit) {
    String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    return this.allStreams().thenApply(stations -> {
      List<RadioStation> filtered = new ArrayList<>();
      for (RadioStation station : stations) {
        if (needle.isBlank() || station.name().toLowerCase(Locale.ROOT).contains(needle)) {
          filtered.add(station);
        }
      }
      return page(filtered, offset, limit);
    });
  }

  @Override
  public CompletableFuture<StationPage> discover(int offset, int limit) {
    return this.allStreams().thenApply(stations -> page(stations, offset, limit));
  }

  @Override
  public CompletableFuture<RadioStation> details(String stationId) {
    long id = RadioRegMapper.providerId(stationId);
    return this.allStreams().thenApply(stations -> {
      for (RadioStation station : stations) {
        if (RadioRegMapper.providerId(station.id()) == id) {
          return station;
        }
      }
      throw new IllegalArgumentException("Unknown RadioReg station " + id);
    });
  }

  @Override
  public CompletableFuture<RadioMetadata> metadata(RadioStation station) {
    long id = RadioRegMapper.providerId(station.id());
    return cached("radioreg:history:" + id, Duration.ofSeconds(4), () ->
        this.http.get(API + "/stream/" + id + "/history")
            .thenApply(json -> RadioRegMapper.history(json.getAsJsonObject()))
    );
  }

  @Override
  public CompletableFuture<List<String>> genres() {
    return this.allStreams().thenApply(stations -> {
      Set<String> genres = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      for (RadioStation station : stations) {
        for (String tag : station.tags()) {
          if (!tag.isBlank()) {
            genres.add(tag);
          }
        }
      }
      return List.copyOf(genres);
    });
  }

  private CompletableFuture<List<RadioStation>> allStreams() {
    return cached("radioreg:streams", Duration.ofMinutes(1), () -> this.http.get(API + "/stream")
        .thenApply(RadioRegProvider::parseStreams));
  }

  static List<RadioStation> parseStreams(JsonElement json) {
    Iterable<JsonElement> items;
    if (json.isJsonArray()) {
      items = json.getAsJsonArray();
    } else if (json.isJsonObject()) {
      JsonObject root = json.getAsJsonObject();
      JsonElement wrapped = root.has("items") ? root.get("items") : root.get("streams");
      if (wrapped == null || !wrapped.isJsonArray()) {
        throw new IllegalStateException("Unexpected RadioReg stream response");
      }
      items = wrapped.getAsJsonArray();
    } else {
      throw new IllegalStateException("Unexpected RadioReg stream response");
    }
    List<JsonObject> source = new ArrayList<>();
    for (JsonElement element : items) {
      if (element.isJsonObject()) {
        source.add(element.getAsJsonObject());
      }
    }
    source.sort(Comparator
        .comparingInt(RadioRegProvider::discoveryPriority).reversed()
        .thenComparing(object -> LautFmMapper.string(object, "name"), String.CASE_INSENSITIVE_ORDER));
    List<RadioStation> stations = new ArrayList<>();
    for (JsonObject element : source) {
      RadioStation station = RadioRegMapper.station(element);
      if (!station.name().isBlank()
          && !station.streamUrl().isBlank()
          && isSupportedStream(station.streamUrl())) {
        stations.add(station);
      }
    }
    return List.copyOf(stations);
  }

  private static int discoveryPriority(JsonObject stream) {
    int value = 0;
    value += flag(stream, "isStreamOfTheMonth") ? 8 : 0;
    value += flag(stream, "isRecentPopularStream") ? 4 : 0;
    value += flag(stream, "isTopVotedStream") ? 2 : 0;
    value += flag(stream, "isNewcomerOfTheMonth") ? 1 : 0;
    return value;
  }

  private static boolean flag(JsonObject object, String key) {
    JsonElement value = object.get(key);
    return value != null && !value.isJsonNull() && value.getAsBoolean();
  }

  private static boolean isSupportedStream(String url) {
    return StreamFormatDetector.fromUrl(url).canAttemptPlayback();
  }

  private static StationPage page(List<RadioStation> stations, int offset, int limit) {
    int safeOffset = Math.max(0, Math.min(offset, stations.size()));
    int safeLimit = Math.max(1, limit);
    int end = Math.min(stations.size(), safeOffset + safeLimit);
    return new StationPage(stations.subList(safeOffset, end), safeOffset, safeLimit, stations.size());
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
