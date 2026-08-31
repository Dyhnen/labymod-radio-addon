package net.dyhntastic.radio.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TtlCacheTest {

  @Test
  void usesFreshValueAndFallsBackToStaleValue() {
    MutableClock clock = new MutableClock();
    TtlCache<String, String> cache = new TtlCache<>(clock);
    AtomicInteger loads = new AtomicInteger();

    String first = cache.getOrLoad("key", Duration.ofSeconds(1), () -> {
      loads.incrementAndGet();
      return CompletableFuture.completedFuture("value");
    }).join();
    clock.advance(Duration.ofSeconds(2));
    String second = cache.getOrLoad("key", Duration.ofSeconds(1), () -> {
      loads.incrementAndGet();
      return CompletableFuture.failedFuture(new IllegalStateException("offline"));
    }).join();

    assertEquals("value", first);
    assertEquals("value", second);
    assertEquals(2, loads.get());
  }

  @Test
  void evictsOldestEntryAtConfiguredLimit() {
    TtlCache<String, String> cache = new TtlCache<>(Clock.systemUTC(), 2);
    cache.put("oldest", "one", Duration.ofMinutes(1));
    cache.put("middle", "two", Duration.ofMinutes(2));
    cache.put("newest", "three", Duration.ofMinutes(3));

    assertFalse(cache.fresh("oldest").isPresent());
    assertTrue(cache.fresh("middle").isPresent());
    assertTrue(cache.fresh("newest").isPresent());
  }

  private static final class MutableClock extends Clock {

    private Instant instant = Instant.parse("2026-08-31T00:00:00Z");

    void advance(Duration duration) {
      this.instant = this.instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return this.instant;
    }
  }
}
