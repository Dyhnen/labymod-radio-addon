package net.dyhntastic.radio.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TtlCacheTest {

  @Test
  void usesFreshValueAndFallsBackToStaleValue() {
    TtlCache<String, String> cache = new TtlCache<>();
    AtomicInteger loads = new AtomicInteger();

    String first = cache.getOrLoad("key", Duration.ofMinutes(1), () -> {
      loads.incrementAndGet();
      return CompletableFuture.completedFuture("value");
    }).join();
    String second = cache.getOrLoad("key", Duration.ofMinutes(1), () -> {
      loads.incrementAndGet();
      return CompletableFuture.failedFuture(new IllegalStateException("offline"));
    }).join();

    assertEquals("value", first);
    assertEquals("value", second);
    assertEquals(1, loads.get());
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
}
