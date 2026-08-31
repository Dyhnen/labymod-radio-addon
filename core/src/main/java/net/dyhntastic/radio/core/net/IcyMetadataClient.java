package net.dyhntastic.radio.core.net;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dyhntastic.radio.api.RadioMetadata;
import net.dyhntastic.radio.core.RadioConstants;

/** Reads the StreamTitle block exposed by Shoutcast/Icecast MP3 streams. */
public final class IcyMetadataClient {

  private static final Pattern STREAM_TITLE = Pattern.compile("StreamTitle='([^']*)'", Pattern.CASE_INSENSITIVE);
  private static final int MAX_METADATA_INTERVAL = 2 * 1024 * 1024;
  private final Executor executor;

  public IcyMetadataClient(Executor executor) {
    this.executor = executor;
  }

  public CompletableFuture<RadioMetadata> fetch(String streamUrl) {
    return CompletableFuture.supplyAsync(() -> this.fetchBlocking(streamUrl), this.executor);
  }

  private RadioMetadata fetchBlocking(String streamUrl) {
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) URI.create(streamUrl).toURL().openConnection();
      connection.setConnectTimeout(5_000);
      connection.setReadTimeout(6_000);
      connection.setInstanceFollowRedirects(true);
      connection.setRequestProperty("Icy-MetaData", "1");
      connection.setRequestProperty("Accept", "audio/mpeg,audio/*;q=0.8,*/*;q=0.1");
      connection.setRequestProperty("User-Agent", RadioConstants.USER_AGENT);
      int status = connection.getResponseCode();
      if (status < 200 || status >= 300) {
        throw new IllegalStateException("HTTP " + status + " for " + streamUrl);
      }
      int metadataInterval = parseInterval(connection.getHeaderField("icy-metaint"));
      if (metadataInterval <= 0 || metadataInterval > MAX_METADATA_INTERVAL) {
        return RadioMetadata.EMPTY;
      }
      try (InputStream input = new BufferedInputStream(connection.getInputStream(), 64 * 1024)) {
        byte[] audio = new byte[Math.min(metadataInterval, 32 * 1024)];
        for (int attempt = 0; attempt < 2; attempt++) {
          readExactly(input, audio, metadataInterval);
          int blocks = input.read();
          if (blocks < 0) {
            return RadioMetadata.EMPTY;
          }
          int length = blocks * 16;
          if (length == 0) {
            continue;
          }
          byte[] bytes = input.readNBytes(length);
          if (bytes.length != length) {
            return RadioMetadata.EMPTY;
          }
          String raw = new String(bytes, StandardCharsets.ISO_8859_1).replace("\u0000", "").trim();
          Matcher matcher = STREAM_TITLE.matcher(raw);
          if (matcher.find() && !matcher.group(1).isBlank()) {
            return parseStreamTitle(matcher.group(1));
          }
        }
        return RadioMetadata.EMPTY;
      }
    } catch (Exception exception) {
      throw new IllegalStateException("Could not read ICY metadata for " + streamUrl, exception);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  static RadioMetadata parseStreamTitle(String streamTitle) {
    String value = streamTitle == null ? "" : streamTitle.strip();
    if (value.isEmpty()) {
      return RadioMetadata.EMPTY;
    }
    int separator = value.indexOf(" - ");
    if (separator <= 0 || separator >= value.length() - 3) {
      return new RadioMetadata(value, "", "", "", "");
    }
    return new RadioMetadata(
        value.substring(separator + 3).strip(),
        value.substring(0, separator).strip(),
        "",
        "",
        ""
    );
  }

  private static int parseInterval(String value) {
    if (value == null || value.isBlank()) {
      return -1;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException exception) {
      return -1;
    }
  }

  private static void readExactly(InputStream input, byte[] buffer, int length) throws Exception {
    int remaining = length;
    while (remaining > 0) {
      int read = input.read(buffer, 0, Math.min(buffer.length, remaining));
      if (read < 0) {
        throw new IllegalStateException("Stream ended before its ICY metadata block");
      }
      remaining -= read;
    }
  }
}
