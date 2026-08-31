package net.dyhntastic.radio.core.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import java.util.List;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;
import org.junit.jupiter.api.Test;

class RadioRegMapperTest {

  @Test
  void mapsCurrentNextCoverAndOrganization() {
    var json = JsonParser.parseString("""
        {
          "id":5,
          "name":"Gaming",
          "url":"https://listen.example/gaming.mp3",
          "tags":["Electronic","Dance"],
          "country":"DE",
          "organization":{"name":"Example","image":"https://cdn.example/logo.png"},
          "song":{"title":"Current","artist":"Artist","cover":"https://cdn.example/cover.png",
            "next":{"title":"Next","artist":"Next Artist"}}
        }
        """).getAsJsonObject();

    RadioStation station = RadioRegMapper.station(json);

    assertEquals("radioreg:5", station.id());
    assertEquals(RadioSource.RADIOREG, station.source());
    assertEquals("Electronic", station.genre());
    assertEquals("Current", station.metadata().title());
    assertEquals("Next", station.metadata().nextTitle());
    assertEquals("Next Artist", station.metadata().nextArtist());
  }

  @Test
  void mapsCurrentPublicStreamList() {
    var json = JsonParser.parseString("""
        [
          {"id":5,"name":"Gaming","url":"https://listen.example/gaming.m3u8",
           "tags":["Electronic"],"organization":{"name":"atomicradio","image":"https://cdn.example/logo.png"},
           "song":{"title":"Current","artist":"Artist","next":null},"isTopVotedStream":true},
          {"id":720,"name":"Hyperion Radio","url":"https://mira.example/radio.mp3",
           "tags":["Pop"],"organization":{"name":"TitanReach Media"},"song":null,
           "isStreamOfTheMonth":true}
        ]
        """);

    List<RadioStation> stations = RadioRegProvider.parseStreams(json);

    assertEquals(1, stations.size());
    assertEquals("radioreg:720", stations.getFirst().id());
  }
}
