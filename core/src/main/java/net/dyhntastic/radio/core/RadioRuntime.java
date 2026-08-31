package net.dyhntastic.radio.core;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.dyhntastic.radio.api.PlayerState;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioPlayer;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;
import net.dyhntastic.radio.api.StationPage;
import net.dyhntastic.radio.api.StationProvider;
import net.dyhntastic.radio.core.audio.JLayerRadioPlayer;
import net.dyhntastic.radio.core.audio.StreamFormat;
import net.dyhntastic.radio.core.audio.StreamFormatDetector;
import net.dyhntastic.radio.core.cover.CoverIconCache;
import net.dyhntastic.radio.core.net.HttpJsonClient;
import net.dyhntastic.radio.core.net.IcyMetadataClient;
import net.dyhntastic.radio.core.persistence.StationRepository;
import net.dyhntastic.radio.core.presence.RadioPresenceService;
import net.dyhntastic.radio.core.presence.RemoteRadioPresence;
import net.dyhntastic.radio.core.provider.CustomStationProvider;
import net.dyhntastic.radio.core.provider.DyhnunityStations;
import net.dyhntastic.radio.core.provider.ILoveMusicProvider;
import net.dyhntastic.radio.core.provider.LautFmProvider;
import net.dyhntastic.radio.core.provider.RadioBrowserProvider;
import net.dyhntastic.radio.core.provider.RadioRegProvider;
import net.dyhntastic.radio.core.util.SearchDebouncer;
import net.dyhntastic.radio.core.util.TtlCache;
import net.dyhntastic.radio.core.util.UrlValidator;
import net.labymod.api.client.gui.icon.Icon;

public final class RadioRuntime implements AutoCloseable {

  private static final int MAX_CUSTOM_NAME_LENGTH = 80;
  private static final int MAX_CUSTOM_URL_LENGTH = 2_048;
  private final DyhnunityRadioAddon addon;
  private final RadioConfiguration configuration;
  private final ExecutorService networkExecutor = Executors.newFixedThreadPool(6, runnable -> {
    Thread thread = new Thread(runnable, "dyhnunity-radio-network");
    thread.setDaemon(true);
    return thread;
  });
  private final ExecutorService audioExecutor = Executors.newSingleThreadExecutor(runnable -> {
    Thread thread = new Thread(runnable, "dyhnunity-radio-audio");
    thread.setDaemon(true);
    return thread;
  });
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
    Thread thread = new Thread(runnable, "dyhnunity-radio-scheduler");
    thread.setDaemon(true);
    return thread;
  });
  private final HttpJsonClient http = new HttpJsonClient(this.networkExecutor);
  private final IcyMetadataClient icy = new IcyMetadataClient(this.networkExecutor);
  private final TtlCache<String, Object> cache = new TtlCache<>();
  private final Map<RadioSource, StationProvider> providers = new EnumMap<>(RadioSource.class);
  private final SearchDebouncer searchDebouncer = new SearchDebouncer(this.scheduler, Duration.ofMillis(400));
  private final AtomicReference<RadioStation> currentStation = new AtomicReference<>();
  private final AtomicLong metadataGeneration = new AtomicLong();
  private final Set<String> metadataCheckedStations = ConcurrentHashMap.newKeySet();
  private final Gson gson = new Gson();
  private final StationRepository repository;
  private final JLayerRadioPlayer player;
  private final RadioPresenceService presence;
  private final CoverIconCache coverIcons;
  private volatile boolean closed;

  public RadioRuntime(DyhnunityRadioAddon addon, RadioConfiguration configuration) {
    this.addon = addon;
    this.configuration = configuration;
    this.repository = new StationRepository(
        configuration.favoritesJson().get(),
        configuration.customStationsJson().get(),
        DyhnunityStations.defaultFavorites(),
        (favorites, custom) -> {
          configuration.favoritesJson().set(favorites);
          configuration.customStationsJson().set(custom);
          this.saveConfiguration();
        }
    );
    this.player = new JLayerRadioPlayer(this.audioExecutor);
    this.player.setVolume(configuration.volume().get() / 100.0F);
    configuration.volume().addChangeListener(value -> this.player.setVolume(value / 100.0F));
    this.presence = new RadioPresenceService(addon.labyAPI(), configuration);
    this.coverIcons = new CoverIconCache();

    this.providers.put(RadioSource.LAUTFM, new LautFmProvider(this.http, this.cache));
    this.providers.put(
        RadioSource.RADIO_BROWSER,
        new RadioBrowserProvider(this.http, this.icy, this.cache)
    );
    this.providers.put(RadioSource.ILOVE_MUSIC, new ILoveMusicProvider(this.icy, this.cache));
    this.providers.put(RadioSource.RADIOREG, new RadioRegProvider(this.http, this.cache));
    this.providers.put(RadioSource.CUSTOM, new CustomStationProvider(this.repository));
    this.scheduler.scheduleWithFixedDelay(this::refreshMetadata, 1, 5, TimeUnit.SECONDS);
    this.scheduler.scheduleWithFixedDelay(
        () -> this.presence.synchronize(this.currentStation.get(), this.player.state()),
        1,
        1,
        TimeUnit.SECONDS
    );
    this.scheduler.scheduleAtFixedRate(
        () -> addon.labyAPI().minecraft().executeOnRenderThread(this.coverIcons::retryFailed),
        5,
        5,
        TimeUnit.SECONDS
    );
  }

  public void start() {
    if (!this.configuration.autoPlayLastStation().get()) {
      return;
    }
    RadioStation last = this.lastStation();
    if (last != null && UrlValidator.isHttpUrl(last.streamUrl())) {
      this.play(last);
    }
  }

  public void play(RadioStation station) {
    if (station == null) {
      return;
    }
    this.metadataGeneration.incrementAndGet();
    this.currentStation.set(station);
    this.player.switchStation(station);
    this.repository.updateFavoriteSnapshot(station);
    if (this.configuration.rememberLastStation().get()) {
      this.configuration.lastStationJson().set(this.gson.toJson(station));
      this.saveConfiguration();
    }
    this.refreshMetadata();
  }

  public void togglePause() {
    if (this.player.state() == PlayerState.PAUSED) {
      this.player.resume();
    } else if (this.player.state() == PlayerState.PLAYING
        || this.player.state() == PlayerState.LOADING) {
      this.player.pause();
    } else if (this.currentStation.get() != null) {
      this.play(this.currentStation.get());
    }
  }

  public void stop() {
    this.metadataGeneration.incrementAndGet();
    this.player.stop();
  }

  public void reconnect() {
    RadioStation station = this.currentStation.get();
    if (station == null) {
      return;
    }
    this.metadataGeneration.incrementAndGet();
    this.player.play(station);
    this.refreshMetadata();
  }

  public void setVolume(float percent) {
    float safe = Math.max(0.0F, Math.min(100.0F, percent));
    this.configuration.volume().set(safe);
    this.player.setVolume(safe / 100.0F);
    this.saveConfiguration();
  }

  public Icon coverIcon(String url) {
    return this.coverIcons.icon(url);
  }

  public CompletableFuture<StationPage> discover(RadioSource source, int offset, int limit) {
    return this.provider(source).discover(offset, limit);
  }

  public CompletableFuture<StationPage> search(
      RadioSource source,
      String query,
      int offset,
      int limit
  ) {
    return this.searchDebouncer.submit(() -> this.provider(source).search(query, offset, limit));
  }

  public CompletableFuture<List<RadioStation>> loadPreviews(List<RadioStation> stations) {
    List<CompletableFuture<RadioStation>> previews = new java.util.ArrayList<>(stations.size());
    for (RadioStation station : stations) {
      previews.add(this.loadPreview(station));
    }
    CompletableFuture<?>[] pending = previews.toArray(new CompletableFuture<?>[0]);
    return CompletableFuture.allOf(pending).thenApply(ignored -> {
      List<RadioStation> loaded = new java.util.ArrayList<>(previews.size());
      for (CompletableFuture<RadioStation> preview : previews) {
        loaded.add(preview.join());
      }
      return List.copyOf(loaded);
    });
  }

  private CompletableFuture<RadioStation> loadPreview(RadioStation station) {
    RadioMetadata metadata = station.metadata();
    if (station.source() == RadioSource.CUSTOM) {
      this.metadataCheckedStations.add(station.id());
      return CompletableFuture.completedFuture(station);
    }
    if (!metadata.artist().isBlank() && !metadata.title().isBlank()) {
      this.metadataCheckedStations.add(station.id());
      return CompletableFuture.completedFuture(station);
    }
    return this.refreshPreview(station);
  }

  /**
   * Loads a fresh metadata snapshot even when a persisted station already contains an old one.
   */
  public CompletableFuture<RadioStation> refreshPreview(RadioStation station) {
    if (station.source() == RadioSource.CUSTOM) {
      return CompletableFuture.completedFuture(station);
    }
    return this.provider(station.source()).metadata(station).handle((metadata, error) -> {
      this.metadataCheckedStations.add(station.id());
      return error == null ? station.withMetadata(metadata) : station;
    });
  }

  public boolean metadataChecked(String stationId) {
    return stationId != null && this.metadataCheckedStations.contains(stationId);
  }

  public RadioStation saveCustom(
      String existingId,
      String name,
      String streamUrl
  ) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Station name is required");
    }
    if (name.trim().length() > MAX_CUSTOM_NAME_LENGTH) {
      throw new IllegalArgumentException("Station name is too long");
    }
    if (streamUrl != null && streamUrl.trim().length() > MAX_CUSTOM_URL_LENGTH) {
      throw new IllegalArgumentException("Stream URL is too long");
    }
    if (!UrlValidator.isHttpUrl(streamUrl)) {
      throw new IllegalArgumentException("Stream URL must be HTTP or HTTPS");
    }
    StreamFormat format = StreamFormatDetector.fromUrl(streamUrl);
    if (!format.canAttemptPlayback()) {
      throw new IllegalArgumentException("Unsupported stream format: " + format.displayName());
    }
    String id = existingId == null || existingId.isBlank()
        ? "custom:" + UUID.randomUUID()
        : existingId;
    RadioStation station = new RadioStation(
        id,
        name.trim(),
        RadioSource.CUSTOM,
        streamUrl.trim(),
        "",
        "",
        List.of("Custom"),
        "",
        "",
        "",
        RadioMetadata.EMPTY
    );
    this.repository.saveCustom(station);
    return station;
  }

  public void deleteCustom(String stationId) {
    this.repository.deleteCustom(stationId);
  }

  public void toggleFavorite(RadioStation station) {
    this.repository.toggleFavorite(station);
  }

  public boolean isFavorite(String stationId) {
    return this.repository.isFavorite(stationId);
  }

  public List<RadioStation> favorites() {
    return this.repository.favorites();
  }

  public List<RadioStation> customStations() {
    return this.repository.customStations();
  }

  public RadioStation currentStation() {
    return this.currentStation.get();
  }

  public RadioPlayer player() {
    return this.player;
  }

  public String knownUnsupportedFormat(RadioStation station) {
    if (station == null) {
      return "";
    }
    StreamFormat format = StreamFormatDetector.fromUrl(station.streamUrl());
    return format.canAttemptPlayback() ? "" : format.displayName();
  }

  public RadioConfiguration configuration() {
    return this.configuration;
  }

  public RadioPresenceService presenceService() {
    return this.presence;
  }

  public RemoteRadioPresence remotePresence(UUID playerId) {
    return this.presence.remotePresence(playerId);
  }

  public StationProvider provider(RadioSource source) {
    StationProvider provider = this.providers.get(source);
    if (provider == null) {
      throw new IllegalArgumentException("No provider for " + source);
    }
    return provider;
  }

  public void refreshMetadata() {
    RadioStation station = this.currentStation.get();
    if (station == null || this.closed) {
      return;
    }
    long requestGeneration = this.metadataGeneration.get();
    String stationId = station.id();
    this.provider(station.source()).metadata(station).whenComplete((metadata, error) -> {
      RadioStation current = this.currentStation.get();
      if (this.metadataGeneration.get() != requestGeneration
          || current == null
          || !stationId.equals(current.id())) {
        return;
      }
      this.metadataCheckedStations.add(stationId);
      if (error != null || metadata == null) {
        return;
      }
      RadioStation updated = current.withMetadata(metadata);
      this.currentStation.set(updated);
      this.repository.updateFavoriteSnapshot(updated);
    });
  }

  @Override
  public void close() {
    if (this.closed) {
      return;
    }
    this.closed = true;
    this.searchDebouncer.cancel();
    this.metadataGeneration.incrementAndGet();
    this.player.close();
    this.presence.close();
    this.http.close();
    this.scheduler.shutdownNow();
    this.networkExecutor.shutdownNow();
    this.audioExecutor.shutdownNow();
  }

  private RadioStation lastStation() {
    String value = this.configuration.lastStationJson().get();
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return this.gson.fromJson(value, RadioStation.class);
    } catch (JsonSyntaxException exception) {
      return null;
    }
  }

  private void saveConfiguration() {
    try {
      this.addon.saveConfiguration();
    } catch (Exception exception) {
      this.addon.logger().error("Could not save Dyhnunity Radio Player configuration", exception);
    }
  }
}
