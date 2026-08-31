package net.dyhntastic.radio.core.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.dyhntastic.radio.api.RadioMetadata;
import org.junit.jupiter.api.Test;

class IcyMetadataClientTest {

  @Test
  void splitsArtistAndTitle() {
    RadioMetadata metadata = IcyMetadataClient.parseStreamTitle("Kygo & Whitney Houston - Higher Love");

    assertEquals("Kygo & Whitney Houston", metadata.artist());
    assertEquals("Higher Love", metadata.title());
  }

  @Test
  void keepsUnknownFormatAsTitle() {
    RadioMetadata metadata = IcyMetadataClient.parseStreamTitle("I LOVE RADIO");

    assertEquals("", metadata.artist());
    assertEquals("I LOVE RADIO", metadata.title());
  }
}
