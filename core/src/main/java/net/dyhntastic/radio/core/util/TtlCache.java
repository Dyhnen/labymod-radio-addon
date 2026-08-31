package net.dyhntastic.radio.core.util;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class TtlCache<K, V> {

  private static final int DEFAULT_MAX_ENTRIES = 256;
  private final Map<K, Entry<V>> entries = new ConcurrentHashMap<>();
  private final Clock clock;
  private final int maxEntries;

  public TtlCache() {
    this(Clock.systemUTC(), DEFAULT_MAX_ENTRIES);
  }

  TtlCache(Clock clock) {
    this(clock, DEFAULT_MAX_ENTRIES);
  }

  TtlCache(Clock clock, int maxEntries) {
    this.clock = clock;
    this.maxEntries = Math.max(1, maxEntries);
  }

  public Optional<V> fresh(K key) {
    Entry<V> entry = this.entries.get(key);
    if (entry == null || entry.expiresAtMillis() < this.clock.millis()) {
      return Optional.empty();
    }
    return Optional.of(entry.value());
  }

  public Optional<V> stale(K key) {
    Entry<V> entry = this.entries.get(key);
    return entry == null ? Optional.empty() : Optional.of(entry.value());
  }

  public void put(K key, V value, Duration ttl) {
    long now = this.clock.millis();
    this.removeExpired(now);
    if (!this.entries.containsKey(key) && this.entries.size() >= this.maxEntries) {
      this.removeOldest();
    }
    this.entries.put(key, new Entry<>(value, now + Math.max(1, ttl.toMillis())));
  }

  public CompletableFuture<V> getOrLoad(
      K key,
      Duration ttl,
      Supplier<CompletableFuture<V>> loader
  ) {
    Optional<V> cached = this.fresh(key);
    if (cached.isPresent()) {
      return CompletableFuture.completedFuture(cached.get());
    }

    return loader.get().handle((value, error) -> {
      if (error == null) {
        this.put(key, value, ttl);
        return value;
      }
      return this.stale(key).orElseThrow(() -> new CacheLoadException(error));
    });
  }

  public void clear() {
    this.entries.clear();
  }

  private void removeExpired(long now) {
    for (Map.Entry<K, Entry<V>> cached : this.entries.entrySet()) {
      if (cached.getValue().expiresAtMillis() < now) {
        this.entries.remove(cached.getKey(), cached.getValue());
      }
    }
  }

  private void removeOldest() {
    K oldestKey = null;
    Entry<V> oldest = null;
    for (Map.Entry<K, Entry<V>> cached : this.entries.entrySet()) {
      if (oldest == null || cached.getValue().expiresAtMillis() < oldest.expiresAtMillis()) {
        oldestKey = cached.getKey();
        oldest = cached.getValue();
      }
    }
    if (oldestKey != null) {
      this.entries.remove(oldestKey, oldest);
    }
  }

  private record Entry<V>(V value, long expiresAtMillis) {
  }

  public static final class CacheLoadException extends RuntimeException {

    public CacheLoadException(Throwable cause) {
      super(cause);
    }
  }
}
