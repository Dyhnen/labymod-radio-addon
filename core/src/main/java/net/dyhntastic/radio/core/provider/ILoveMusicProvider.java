package net.dyhntastic.radio.core.provider;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;
import net.dyhntastic.radio.api.StationPage;
import net.dyhntastic.radio.api.StationProvider;
import net.dyhntastic.radio.core.net.IcyMetadataClient;
import net.dyhntastic.radio.core.util.TtlCache;

/** Curated catalog of the official I LOVE MUSIC MP3 channels. */
public final class ILoveMusicProvider implements StationProvider {

  private static final String HOME = "https://ilovemusic.de/";
  private static final String LOGO = "https://ilovemusic.de/favicon.png";
  private static final List<RadioStation> STATIONS = List.of(
      station("radio", "I LOVE RADIO", "ilm_iloveradio", "Pop"),
      station("2dance", "I LOVE 2 DANCE", "ilm_ilove2dance", "Dance"),
      station("2000", "I LOVE 2000+ THROWBACKS", "ilm_ilove2000throwbacks", "2000s"),
      station("2010", "I LOVE 2010+ THROWBACKS", "ilm_ilove2010throwbacks", "2010s"),
      station("bass", "I LOVE BASS BY HBZ", "ilm_ilovebass", "Bass"),
      station("newpop", "I LOVE BIGGEST POP HITS", "ilm_ilovenewpop", "Pop"),
      stationWithPath("buffelradio", "I LOVE BUFFELRADIO", "ilm-buffelradio/mp3-192", "Party"),
      station("chillhop", "I LOVE CHILLHOP", "ilm_ilovechillhop", "Chillhop"),
      stationWithPath("chillout", "I LOVE CHILLOUT BEATS", "ilm-ichillout_beats", "Chillout"),
      station("dance-current", "I LOVE DANCE 2026", "ilm_dance-2023-jahrescharts", "Dance"),
      station("dance-history", "I LOVE DANCE HISTORY", "ilm_ilovedancehistory", "Dance"),
      station("deutschrap-beste", "I LOVE DEUTSCHRAP BESTE", "ilm_ilovedeutschrapbeste", "Deutschrap"),
      station("deutschrap-first", "I LOVE DEUTSCHRAP FIRST", "ilm_ilovedeutschrapfirst", "Deutschrap"),
      station("greatest-hits", "I LOVE GREATEST HITS", "ilm_ilovegreatesthits", "Hits"),
      station("hardstyle", "I LOVE HARDSTYLE", "ilm_ilovehardstyle", "Hardstyle"),
      station("hiphop", "I LOVE HIP HOP", "ilm_ilovehiphop", "Hip Hop"),
      station("hiphop-current", "I LOVE HIP HOP 2026", "ilm_hiphop-2023-jahrescharts", "Hip Hop"),
      station("hiphop-history", "I LOVE HIP HOP HISTORY", "ilm_ilovehiphophistory", "Hip Hop"),
      stationWithPath("hit-quiz", "I LOVE HIT-QUIZ", "ilm-ihit-quiz", "Quiz"),
      station("hits-current", "I LOVE HITS 2026", "ilm_hits-2023-jahrescharts", "Hits"),
      station("hits-history", "I LOVE HITS HISTORY", "ilm_ilovehitshistory", "Hits"),
      stationWithPath("kpop", "I LOVE K-POP", "ilm-ilovekpop", "K-Pop"),
      station("mainstage", "I LOVE MAINSTAGE", "ilm_ilovemainstagemadness", "Festival"),
      station("malle", "I LOVE MALLE", "ilm_ilovemalle", "Party"),
      station("mashup", "I LOVE MASHUP", "ilm_ilovemashup", "Mashup"),
      station("music-chill", "I LOVE MUSIC&CHILL", "ilm_ilovemusicandchill", "Chillout"),
      station("party-hard", "I LOVE PARTY HARD", "ilm_ilovepartyhard", "Party"),
      station("rock", "I LOVE ROCK RADIO", "ilm_iloveradiorock", "Rock"),
      station("sugar", "I LOVE SUGAR RADIO", "ilm_ilovesugarradio", "Pop"),
      station("90s", "I LOVE THE 90S", "ilm_ilovethe90s", "90s"),
      station("beach", "I LOVE THE BEACH", "ilm_ilovethebeach", "Chillout"),
      stationWithPath("summer", "I LOVE THE SUMMER", "ilm-ilovethesummer/mp3-192", "Summer"),
      station("sun", "I LOVE THE SUN", "ilm_ilovethesun", "Summer"),
      stationWithPath("tomorrowland", "I LOVE TOMORROWLAND ONE WORLD RADIO", "ilm-itomorrowland_one_world_radio_germany", "Festival"),
      station("top100", "I LOVE TOP 100 CHARTS", "ilm_ilovetop100charts", "Charts"),
      station("trashpop", "I LOVE TRASHPOP", "ilm_ilovetrashpop", "Pop"),
      station("us-rap", "I LOVE US ONLY RAP RADIO", "ilm_iloveusonlyrapradio", "Rap"),
      station("workout", "I LOVE WORKOUT", "ilm_iloveworkout", "Workout"),
      station("xmas", "I LOVE X-MAS", "ilm_ilovexmas", "Christmas")
  );

  private final IcyMetadataClient icy;
  private final TtlCache<String, Object> cache;

  public ILoveMusicProvider(IcyMetadataClient icy, TtlCache<String, Object> cache) {
    this.icy = icy;
    this.cache = cache;
  }

  @Override
  public RadioSource source() {
    return RadioSource.ILOVE_MUSIC;
  }

  @Override
  public CompletableFuture<StationPage> search(String query, int offset, int limit) {
    String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    List<RadioStation> matches = new java.util.ArrayList<>();
    for (RadioStation station : STATIONS) {
      if (needle.isBlank() || station.name().toLowerCase(Locale.ROOT).contains(needle)) {
        matches.add(station);
      }
    }
    return CompletableFuture.completedFuture(page(matches, offset, limit));
  }

  @Override
  public CompletableFuture<StationPage> discover(int offset, int limit) {
    return CompletableFuture.completedFuture(page(STATIONS, offset, limit));
  }

  @Override
  public CompletableFuture<RadioStation> details(String stationId) {
    for (RadioStation station : STATIONS) {
      if (station.id().equals(stationId)) {
        return CompletableFuture.completedFuture(station);
      }
    }
    return CompletableFuture.failedFuture(
        new IllegalArgumentException("Unknown I LOVE MUSIC station " + stationId)
    );
  }

  @Override
  public CompletableFuture<RadioMetadata> metadata(RadioStation station) {
    return cached("ilove:metadata:" + station.id(), Duration.ofSeconds(4), () ->
        this.icy.fetch(station.streamUrl())
    );
  }

  private static RadioStation station(String id, String name, String stream, String genre) {
    return stationWithPath(id, name, stream, genre);
  }

  private static RadioStation stationWithPath(String id, String name, String stream, String genre) {
    return new RadioStation(
        "ilove:" + id,
        name,
        RadioSource.ILOVE_MUSIC,
        "https://play.ilovemusic.de/" + stream + '/',
        HOME,
        LOGO,
        List.of(genre),
        genre,
        "DE",
        "German",
        RadioMetadata.EMPTY
    );
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
