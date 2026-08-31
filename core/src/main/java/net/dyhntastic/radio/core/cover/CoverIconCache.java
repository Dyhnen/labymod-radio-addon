package net.dyhntastic.radio.core.cover;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.dyhntastic.radio.core.util.UrlValidator;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.CompletableResourceLocation;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.client.resources.texture.TextureDetails;
import net.labymod.api.client.resources.texture.TextureRepository;

/**
 * Reuses LabyMod's persistent remote-texture storage under stable resource keys and retries
 * downloads that failed because of temporary CDN errors or rate limits.
 */
public final class CoverIconCache {

  private static final ResourceLocation FALLBACK = ResourceLocation.create(
      "dyhnunity-radio", "textures/fallback-radio.png"
  );
  private static final long[] RETRY_DELAYS = {5_000L, 15_000L, 30_000L, 60_000L, 120_000L};
  private static final long STALLED_AFTER = 45_000L;
  private static final int MAX_TRACKED_COVERS = 256;

  private final TextureRepository textures;
  private final Map<String, Entry> entries = new ConcurrentHashMap<>();

  public CoverIconCache() {
    this.textures = Laby.references().textureRepository();
  }

  public Icon icon(String url) {
    if (!UrlValidator.isHttpUrl(url)) {
      return Icon.texture(FALLBACK);
    }
    String normalized = url.trim();
    Entry entry = this.entries.get(normalized);
    if (entry == null) {
      this.trimTrackedCovers();
      entry = this.entries.computeIfAbsent(normalized, this::createEntry);
    }
    long now = System.currentTimeMillis();
    entry.lastAccessAt = now;
    this.retryIfRequired(entry, now);
    return entry.icon;
  }

  public void retryFailed() {
    long now = System.currentTimeMillis();
    for (Entry entry : this.entries.values()) {
      this.retryIfRequired(entry, now);
    }
  }

  private Entry createEntry(String url) {
    ResourceLocation location = ResourceLocation.create(
        "dyhnunity-radio", "cover-cache/" + sha256(url)
    );
    Entry entry = new Entry(url, location);
    this.load(entry, false);
    entry.icon = Icon.completable(() -> entry.resource);
    return entry;
  }

  private void trimTrackedCovers() {
    while (this.entries.size() >= MAX_TRACKED_COVERS) {
      String oldestUrl = null;
      Entry oldest = null;
      for (Map.Entry<String, Entry> cached : this.entries.entrySet()) {
        if (oldest == null || cached.getValue().lastAccessAt < oldest.lastAccessAt) {
          oldestUrl = cached.getKey();
          oldest = cached.getValue();
        }
      }
      if (oldestUrl == null || !this.entries.remove(oldestUrl, oldest)) {
        return;
      }
    }
  }

  private void retryIfRequired(Entry entry, long now) {
    CompletableResourceLocation resource = entry.resource;
    if (resource == null || entry.loading) {
      if (entry.loading && now - entry.startedAt >= STALLED_AFTER) {
        this.load(entry, true);
      }
      return;
    }
    if (resource.hasError() && now >= entry.retryAt) {
      this.load(entry, true);
    }
  }

  private synchronized void load(Entry entry, boolean retry) {
    long now = System.currentTimeMillis();
    if (entry.loading && now - entry.startedAt < STALLED_AFTER) {
      return;
    }
    if (retry) {
      this.textures.invalidateRemoteTexturesByLocation(entry.location::equals);
    }
    entry.loading = true;
    entry.startedAt = now;
    TextureDetails details = TextureDetails.builder(entry.location)
        .withFallbackLocation(FALLBACK)
        .withUrl(entry.url)
        .withRegisterStrategy(TextureDetails.RegisterStrategy.REGISTER)
        .build();
    CompletableResourceLocation resource = this.textures.getOrRegisterTexture(details);
    entry.resource = resource;
    resource.addCompletableListener(() -> {
      entry.loading = false;
      if (resource.hasError()) {
        int retryIndex = Math.min(entry.failures, RETRY_DELAYS.length - 1);
        entry.retryAt = System.currentTimeMillis() + RETRY_DELAYS[retryIndex];
        entry.failures++;
      } else {
        entry.failures = 0;
        entry.retryAt = Long.MAX_VALUE;
      }
    });
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private static final class Entry {

    private final String url;
    private final ResourceLocation location;
    private volatile CompletableResourceLocation resource;
    private volatile Icon icon;
    private volatile boolean loading;
    private volatile long startedAt;
    private volatile long retryAt = Long.MAX_VALUE;
    private volatile long lastAccessAt = System.currentTimeMillis();
    private volatile int failures;

    private Entry(String url, ResourceLocation location) {
      this.url = url;
      this.location = location;
    }
  }
}
