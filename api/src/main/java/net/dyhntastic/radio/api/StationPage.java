package net.dyhntastic.radio.api;

import java.util.List;

public record StationPage(List<RadioStation> stations, int offset, int limit, int total) {

  public StationPage {
    stations = stations == null ? List.of() : List.copyOf(stations);
    offset = Math.max(0, offset);
    limit = Math.max(1, limit);
    total = Math.max(stations.size(), total);
  }

  public static StationPage empty(int offset, int limit) {
    return new StationPage(List.of(), offset, limit, 0);
  }
}
