package net.dyhntastic.radio.core.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.dyhntastic.radio.api.PlayerState;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.api.RadioSource;
import net.dyhntastic.radio.api.RadioStation;
import org.junit.jupiter.api.Test;

class JLayerRadioPlayerTest {

  @Test
  void invalidStreamAdvancesSessionAndEntersErrorWithoutWorker() {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      JLayerRadioPlayer player = new JLayerRadioPlayer(executor);
      long before = player.sessionId();
      player.play(new RadioStation(
          "custom:invalid", "Invalid", RadioSource.CUSTOM, "", "", "",
          List.of(), "", "", "", RadioMetadata.EMPTY
      ));

      assertTrue(player.sessionId() > before);
      assertEquals(PlayerState.ERROR, player.state());
      assertEquals(JLayerRadioPlayer.INVALID_STREAM_ERROR, player.errorMessage());

      player.stop();
      assertEquals(PlayerState.STOPPED, player.state());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void knownUnsupportedFormatFailsBeforeStartingNetworkWorker() {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      JLayerRadioPlayer player = new JLayerRadioPlayer(executor);
      player.play(new RadioStation(
          "custom:hls", "HLS", RadioSource.CUSTOM, "https://radio.example/live.m3u8", "", "",
          List.of(), "", "", "", RadioMetadata.EMPTY
      ));

      assertEquals(PlayerState.ERROR, player.state());
      assertEquals(
          JLayerRadioPlayer.UNSUPPORTED_FORMAT_ERROR + "HLS",
          player.errorMessage()
      );
    } finally {
      executor.shutdownNow();
    }
  }
}
