package net.dyhntastic.radio.core.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;
import org.junit.jupiter.api.Test;

class DyhnunityStationsTest {

  @Test
  void exposesNinePlayableLautFmFavorites() {
    List<RadioStation> stations = DyhnunityStations.defaultFavorites();

    assertEquals(9, stations.size());
    HashSet<String> stationIds = new HashSet<>();
    for (RadioStation station : stations) {
      stationIds.add(station.id());
      assertEquals(RadioSource.LAUTFM, station.source());
      assertTrue(station.streamUrl().startsWith("https://"));
      assertTrue(station.homepageUrl().startsWith("https://laut.fm/"));
      assertTrue(!station.logoUrl().isBlank());
    }
    assertEquals(9, stationIds.size());
    assertEquals("laut:dyhnunityfm", stations.getFirst().id());
  }
}
