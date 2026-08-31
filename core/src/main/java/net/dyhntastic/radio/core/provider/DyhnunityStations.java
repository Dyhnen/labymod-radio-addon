package net.dyhntastic.radio.core.provider;

import java.util.List;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;

/** Official DyhnunityFM stations hosted by laut.fm. */
public final class DyhnunityStations {

  private static final List<RadioStation> DEFAULT_FAVORITES = List.of(
      station(
          "dyhnunityfm",
          "Dyhnunity FM",
          "https://dyhnunityfm.stream.laut.fm/dyhnunityfm",
          "https://assets.laut.fm/db9b3558eff0fcd6727948141551d1ab?t=_120x120",
          "Main Station"
      ),
      station(
          "dyhntastic",
          "Dyhntastic",
          "https://dyhntastic.stream.laut.fm/dyhntastic",
          "https://assets.laut.fm/05802f5359b0c1cc6eb27e003456b1c3?t=_120x120",
          "DyhnunityFM"
      ),
      station(
          "dyhnunityfm-gaming",
          "DyhnunityFM Gaming",
          "https://dyhnunityfm-gaming.stream.laut.fm/dyhnunityfm-gaming",
          "https://assets.laut.fm/406e541a5475721adb57815eed47d08d?t=_120x120",
          "Gaming"
      ),
      station(
          "dyhnunityfm-chillout",
          "DyhnunityFM Chillout",
          "https://dyhnunityfm-chillout.stream.laut.fm/dyhnunityfm-chillout",
          "https://assets.laut.fm/def46e76d457eee3f7eb3da845f32f62?t=_120x120",
          "Chillout"
      ),
      station(
          "dyhnunityfm-party",
          "DyhnunityFM Party",
          "https://dyhnunityfm-party.stream.laut.fm/dyhnunityfm-party",
          "https://assets.laut.fm/05c78f46294e2f6101e9697699b89687?t=_120x120",
          "Party"
      ),
      station(
          "dyhnunityfm-schlager",
          "DyhnunityFM Schlager",
          "https://dyhnunityfm-schlager.stream.laut.fm/dyhnunityfm-schlager",
          "https://assets.laut.fm/5a647a5b6e57d9f3f736edf6f1a7d219?t=_120x120",
          "Schlager"
      ),
      station(
          "dyhnunityfm-rap",
          "DyhnunityFM Rap",
          "https://dyhnunityfm-rap.stream.laut.fm/dyhnunityfm-rap",
          "https://assets.laut.fm/9e58abee72ac4bb156d3ff8d17f4f28f?t=_120x120",
          "Rap"
      ),
      station(
          "dyhnunityfm-bass",
          "DyhnunityFM Bass",
          "https://dyhnunityfm-bass.stream.laut.fm/dyhnunityfm-bass",
          "https://assets.laut.fm/a1561775eef7f694eacb34090053bf35?t=_120x120",
          "Bass"
      ),
      station(
          "dyhnunityfm-electro",
          "DyhnunityFM Electro",
          "https://dyhnunityfm-electro.stream.laut.fm/dyhnunityfm-electro",
          "https://assets.laut.fm/b75d4eceff2460ee80cdb9a76bcfa4af?t=_120x120",
          "Electro"
      )
  );

  private DyhnunityStations() {
  }

  public static List<RadioStation> defaultFavorites() {
    return DEFAULT_FAVORITES;
  }

  private static RadioStation station(
      String lautFmName,
      String displayName,
      String streamUrl,
      String logoUrl,
      String description
  ) {
    return new RadioStation(
        "laut:" + lautFmName,
        displayName,
        RadioSource.LAUTFM,
        streamUrl,
        "https://laut.fm/" + lautFmName,
        logoUrl,
        List.of("DyhnunityFM", description),
        description,
        "DE",
        "de",
        RadioMetadata.EMPTY
    );
  }
}
