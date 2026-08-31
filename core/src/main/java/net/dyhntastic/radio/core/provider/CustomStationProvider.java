package net.dyhntastic.radio.core.provider;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;
import net.dyhntastic.radio.api.StationPage;
import net.dyhntastic.radio.api.StationProvider;
import net.dyhntastic.radio.core.persistence.StationRepository;

public final class CustomStationProvider implements StationProvider {

  private final StationRepository repository;

  public CustomStationProvider(StationRepository repository) {
    this.repository = repository;
  }

  @Override
  public RadioSource source() {
    return RadioSource.CUSTOM;
  }

  @Override
  public CompletableFuture<StationPage> search(String query, int offset, int limit) {
    String needle = query == null ? "" : query.toLowerCase(Locale.ROOT);
    List<RadioStation> matches = new java.util.ArrayList<>();
    for (RadioStation station : this.repository.customStations()) {
      if (needle.isBlank() || station.name().toLowerCase(Locale.ROOT).contains(needle)) {
        matches.add(station);
      }
    }
    return CompletableFuture.completedFuture(page(matches, offset, limit));
  }

  @Override
  public CompletableFuture<StationPage> discover(int offset, int limit) {
    return CompletableFuture.completedFuture(page(this.repository.customStations(), offset, limit));
  }

  @Override
  public CompletableFuture<RadioStation> details(String stationId) {
    RadioStation station = this.repository.find(stationId);
    return station == null
        ? CompletableFuture.failedFuture(new IllegalArgumentException("Unknown custom station " + stationId))
        : CompletableFuture.completedFuture(station);
  }

  @Override
  public CompletableFuture<RadioMetadata> metadata(RadioStation station) {
    return CompletableFuture.completedFuture(station.metadata());
  }

  private static StationPage page(List<RadioStation> stations, int offset, int limit) {
    int safeOffset = Math.max(0, Math.min(offset, stations.size()));
    int safeLimit = Math.max(1, limit);
    int end = Math.min(stations.size(), safeOffset + safeLimit);
    return new StationPage(stations.subList(safeOffset, end), safeOffset, safeLimit, stations.size());
  }
}
