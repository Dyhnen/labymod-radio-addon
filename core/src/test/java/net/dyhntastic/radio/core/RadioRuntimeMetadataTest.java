package net.dyhntastic.radio.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.dyhntastic.radio.api.RadioMetadata;
import org.junit.jupiter.api.Test;

class RadioRuntimeMetadataTest {

  @Test
  void keepsLastKnownSongWhenRefreshIsTemporarilyEmpty() {
    RadioMetadata current = new RadioMetadata(
        "Higher Love",
        "Kygo & Whitney Houston",
        "https://images.example/cover.jpg",
        "",
        ""
    );

    assertEquals(
        current,
        RadioRuntime.retainLastKnownNowPlaying(current, RadioMetadata.EMPTY)
    );
  }

  @Test
  void acceptsARealSongChange() {
    RadioMetadata current = new RadioMetadata("Old Title", "Old Artist", "", "", "");
    RadioMetadata update = new RadioMetadata("New Title", "New Artist", "", "", "");

    assertEquals(update, RadioRuntime.retainLastKnownNowPlaying(current, update));
  }
}
