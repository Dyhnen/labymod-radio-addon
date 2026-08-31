package net.dyhntastic.radio.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface StationProvider {

  RadioSource source();

  CompletableFuture<StationPage> search(String query, int offset, int limit);

  CompletableFuture<StationPage> discover(int offset, int limit);

  CompletableFuture<RadioStation> details(String stationId);

  CompletableFuture<RadioMetadata> metadata(RadioStation station);

  default CompletableFuture<List<String>> genres() {
    return CompletableFuture.completedFuture(List.of());
  }
}
