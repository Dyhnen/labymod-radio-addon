package net.dyhntastic.radio.core.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StreamFormatDetectorTest {

  @Test
  void detectsKnownFormatsFromUrlsWithoutBeingConfusedByQueries() {
    assertEquals(
        StreamFormat.MP3,
        StreamFormatDetector.fromUrl("https://radio.example/live.mp3?token=abc")
    );
    assertEquals(StreamFormat.HLS, StreamFormatDetector.fromUrl("https://radio.example/live.m3u8"));
    assertEquals(StreamFormat.PLAYLIST, StreamFormatDetector.fromUrl("https://radio.example/live.pls"));
    assertEquals(StreamFormat.AAC, StreamFormatDetector.fromUrl("https://radio.example/live.aac"));
    assertEquals(StreamFormat.OGG, StreamFormatDetector.fromUrl("https://radio.example/live.opus"));
    assertEquals(StreamFormat.UNKNOWN, StreamFormatDetector.fromUrl("https://radio.example/live"));
  }

  @Test
  void signatureTakesPrecedenceOverIncorrectServerHeaders() {
    byte[] mp3 = new byte[] {'I', 'D', '3', 4, 0, 0};
    assertEquals(
        StreamFormat.MP3,
        StreamFormatDetector.detect("https://radio.example/live.aac", "audio/aacp", mp3, mp3.length)
    );

    byte[] adtsAac = new byte[] {(byte) 0xFF, (byte) 0xF1, 0x50, (byte) 0x80};
    assertEquals(
        StreamFormat.AAC,
        StreamFormatDetector.detect("https://radio.example/live", "audio/mpeg", adtsAac, adtsAac.length)
    );
  }

  @Test
  void detectsPlaylistAndWebResponsesBeforeTheMp3Decoder() {
    byte[] hls = "#EXTM3U\n#EXT-X-VERSION:3".getBytes(StandardCharsets.US_ASCII);
    byte[] html = "<!doctype html><html>".getBytes(StandardCharsets.US_ASCII);

    assertEquals(
        StreamFormat.HLS,
        StreamFormatDetector.detect("https://radio.example/live", "text/plain", hls, hls.length)
    );
    assertEquals(
        StreamFormat.WEB_PAGE,
        StreamFormatDetector.detect("https://radio.example/live", "text/html", html, html.length)
    );
    assertFalse(StreamFormat.HLS.canAttemptPlayback());
    assertTrue(StreamFormat.UNKNOWN.canAttemptPlayback());
  }
}
