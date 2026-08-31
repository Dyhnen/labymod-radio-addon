package net.dyhntastic.radio.core.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import java.util.List;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;
import org.junit.jupiter.api.Test;

class RadioBrowserProviderTest {

  @Test
  void mapsWorkingMp3StationsAndSkipsOtherCodecs() {
    List<RadioStation> stations = RadioBrowserProvider.parseStations(JsonParser.parseString("""
        [
          {
            "stationuuid": "abc-123",
            "name": " Example Radio ",
            "url_resolved": "https://radio.example/live.mp3",
            "homepage": "https://radio.example",
            "favicon": "https://radio.example/icon.png",
            "tags": "pop, hits, pop",
            "countrycode": "DE",
            "language": "german",
            "codec": "MP3"
          },
          {
            "stationuuid": "aac-123",
            "name": "AAC Radio",
            "url_resolved": "https://radio.example/live.aac",
            "codec": "AAC"
          }
        ]
        """));

    assertEquals(1, stations.size());
    RadioStation station = stations.getFirst();
    assertEquals("radio-browser:abc-123", station.id());
    assertEquals("Example Radio", station.name());
    assertEquals(RadioSource.RADIO_BROWSER, station.source());
    assertEquals(List.of("pop", "hits"), station.tags());
  }
}
