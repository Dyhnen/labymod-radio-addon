package net.dyhntastic.radio.core.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executor;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.core.net.IcyMetadataClient;
import net.dyhntastic.radio.core.util.TtlCache;
import org.junit.jupiter.api.Test;

class ILoveMusicProviderTest {

  @Test
  void exposesOfficialCuratedChannelsAndSearchesNamesOnly() {
    Executor direct = Runnable::run;
    ILoveMusicProvider provider = new ILoveMusicProvider(
        new IcyMetadataClient(direct),
        new TtlCache<>()
    );

    var all = provider.discover(0, 100).join();
    var result = provider.search("hardstyle", 0, 24).join();

    assertTrue(all.stations().size() >= 35);
    assertEquals(1, result.stations().size());
    assertEquals("I LOVE HARDSTYLE", result.stations().getFirst().name());
    assertEquals(RadioSource.ILOVE_MUSIC, result.stations().getFirst().source());
  }
}
