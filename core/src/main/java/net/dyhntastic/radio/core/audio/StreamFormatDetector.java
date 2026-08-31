package net.dyhntastic.radio.core.audio;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Detects formats without downloading or buffering the complete radio stream. */
public final class StreamFormatDetector {

  private StreamFormatDetector() {
  }

  public static StreamFormat fromUrl(String url) {
    if (url == null || url.isBlank()) {
      return StreamFormat.UNKNOWN;
    }
    String path = url.strip().toLowerCase(Locale.ROOT);
    int fragment = path.indexOf('#');
    if (fragment >= 0) {
      path = path.substring(0, fragment);
    }
    int query = path.indexOf('?');
    if (query >= 0) {
      path = path.substring(0, query);
    }
    if (path.endsWith(".m3u8")) {
      return StreamFormat.HLS;
    }
    if (path.endsWith(".m3u") || path.endsWith(".pls")) {
      return StreamFormat.PLAYLIST;
    }
    if (path.endsWith(".aac") || path.endsWith(".aacp") || path.endsWith(".m4a")) {
      return StreamFormat.AAC;
    }
    if (path.endsWith(".ogg") || path.endsWith(".oga") || path.endsWith(".opus")) {
      return StreamFormat.OGG;
    }
    if (path.endsWith(".flac")) {
      return StreamFormat.FLAC;
    }
    if (path.endsWith(".wav") || path.endsWith(".wave")) {
      return StreamFormat.WAV;
    }
    if (path.endsWith(".mp3")) {
      return StreamFormat.MP3;
    }
    return StreamFormat.UNKNOWN;
  }

  public static StreamFormat detect(String url, String contentType, byte[] prefix, int length) {
    StreamFormat signature = fromSignature(prefix, length);
    if (signature != StreamFormat.UNKNOWN) {
      return signature;
    }
    StreamFormat header = fromContentType(contentType);
    return header == StreamFormat.UNKNOWN ? fromUrl(url) : header;
  }

  static StreamFormat fromContentType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return StreamFormat.UNKNOWN;
    }
    String value = contentType.toLowerCase(Locale.ROOT);
    int parameters = value.indexOf(';');
    if (parameters >= 0) {
      value = value.substring(0, parameters);
    }
    value = value.trim();
    return switch (value) {
      case "audio/mpeg", "audio/mp3", "audio/x-mpeg", "audio/mpeg3", "audio/x-mpeg-3" ->
          StreamFormat.MP3;
      case "audio/aac", "audio/aacp", "audio/x-aac", "audio/mp4", "audio/x-m4a" ->
          StreamFormat.AAC;
      case "audio/ogg", "application/ogg" -> StreamFormat.OGG;
      case "audio/flac", "audio/x-flac" -> StreamFormat.FLAC;
      case "audio/wav", "audio/wave", "audio/x-wav" -> StreamFormat.WAV;
      case "application/vnd.apple.mpegurl" -> StreamFormat.HLS;
      case "application/x-mpegurl", "audio/mpegurl", "audio/x-mpegurl", "audio/x-scpls" ->
          StreamFormat.PLAYLIST;
      case "text/html", "application/xhtml+xml" -> StreamFormat.WEB_PAGE;
      default -> StreamFormat.UNKNOWN;
    };
  }

  static StreamFormat fromSignature(byte[] prefix, int length) {
    if (prefix == null || length <= 0) {
      return StreamFormat.UNKNOWN;
    }
    int safeLength = Math.min(length, prefix.length);
    if (startsWith(prefix, safeLength, "ID3")) {
      return StreamFormat.MP3;
    }
    if (startsWith(prefix, safeLength, "OggS")) {
      return StreamFormat.OGG;
    }
    if (startsWith(prefix, safeLength, "fLaC")) {
      return StreamFormat.FLAC;
    }
    if (startsWith(prefix, safeLength, "RIFF")
        && safeLength >= 12
        && prefix[8] == 'W'
        && prefix[9] == 'A'
        && prefix[10] == 'V'
        && prefix[11] == 'E') {
      return StreamFormat.WAV;
    }

    String text = new String(prefix, 0, safeLength, StandardCharsets.US_ASCII)
        .stripLeading()
        .toLowerCase(Locale.ROOT);
    if (text.startsWith("#extm3u")) {
      return StreamFormat.HLS;
    }
    if (text.startsWith("[playlist]")) {
      return StreamFormat.PLAYLIST;
    }
    if (text.startsWith("<!doctype html") || text.startsWith("<html")) {
      return StreamFormat.WEB_PAGE;
    }

    if (safeLength >= 2 && (prefix[0] & 0xFF) == 0xFF && (prefix[1] & 0xE0) == 0xE0) {
      int version = (prefix[1] >> 3) & 0x03;
      int layer = (prefix[1] >> 1) & 0x03;
      if (version != 0x01 && layer != 0) {
        return StreamFormat.MP3;
      }
      if (layer == 0) {
        return StreamFormat.AAC;
      }
    }
    return StreamFormat.UNKNOWN;
  }

  private static boolean startsWith(byte[] value, int length, String signature) {
    byte[] expected = signature.getBytes(StandardCharsets.US_ASCII);
    if (length < expected.length) {
      return false;
    }
    for (int index = 0; index < expected.length; index++) {
      if (value[index] != expected[index]) {
        return false;
      }
    }
    return true;
  }
}
