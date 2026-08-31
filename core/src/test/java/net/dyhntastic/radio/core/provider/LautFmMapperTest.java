package net.dyhntastic.radio.core.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;
import net.dyhntastic.radio.api.StationPage;
import org.junit.jupiter.api.Test;

class LautFmMapperTest {

  @Test
  void mapsStationDtoIntoSharedModel() {
    var stationJson = JsonParser.parseString("""
        {
          "name":"demo",
          "display_name":"Demo FM",
          "page_url":"https://laut.fm/demo",
          "stream_url":"https://demo.stream.laut.fm/demo",
          "genres":["Rock","Pop"],
          "images":{"station_120x120":"https://assets.laut.fm/demo"},
          "third_parties":{"website":{"url":"https://demo.example"}}
        }
        """).getAsJsonObject();
    var songJson = JsonParser.parseString("""
        {"title":"Track","artist":{"name":"Artist"}}
        """).getAsJsonObject();

    RadioStation station = LautFmMapper.station(stationJson, songJson);

    assertEquals("laut:demo", station.id());
    assertEquals("Demo FM", station.name());
    assertEquals(RadioSource.LAUTFM, station.source());
    assertEquals("Rock", station.genre());
    assertEquals("Track", station.metadata().title());
    assertEquals("Artist", station.metadata().artist());
  }

  @Test
  void mapsCurrentStationsEnvelope() {
    var json = JsonParser.parseString("""
        {
          "total":15941,
          "offset":0,
          "limit":2,
          "items":[
            {"name":"one","display_name":"One FM","stream_url":"https://one.example/live",
             "genres":["Pop"],"images":{},"third_parties":{}},
            {"name":"two","display_name":"Two FM","stream_url":"https://two.example/live",
             "genres":["Rock"],"images":{},"third_parties":{}}
          ]
        }
        """);

    StationPage page = LautFmProvider.parseDiscovery(json, 0, 2);

    assertEquals(2, page.stations().size());
    assertEquals(15941, page.total());
    assertEquals("laut:one", page.stations().getFirst().id());
  }

  @Test
  void mapsCurrentSearchEnvelopeAndSkipsNonStationItems() {
    var json = JsonParser.parseString("""
        {
          "total":1,
          "offset":0,
          "limit":24,
          "results":[
            {
              "items":[
                {"station":{"name":"gaming","display_name":"Gaming FM",
                  "stream_url":"https://gaming.example/live","genres":["Gaming"],
                  "images":{},"third_parties":{}},
                 "current_song":{"title":"Level Up","artist":{"name":"Player One"}}},
                {"current_song":{"title":"Broken result without station"}}
              ]
            }
          ]
        }
        """);

    StationPage page = LautFmProvider.parseSearch(json, 0, 24);

    assertEquals(1, page.stations().size());
    assertEquals(1, page.total());
    assertEquals("laut:gaming", page.stations().getFirst().id());
    assertEquals("Level Up", page.stations().getFirst().metadata().title());
  }
}
