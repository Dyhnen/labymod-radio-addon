package net.dyhntastic.radio.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SearchDebouncerTest {

  @Test
  void onlyRunsNewestSearch() throws Exception {
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    try {
      SearchDebouncer debouncer = new SearchDebouncer(scheduler, Duration.ofMillis(30));
      CompletableFuture<String> first = debouncer.submit(() -> CompletableFuture.completedFuture("first"));
      CompletableFuture<String> second = debouncer.submit(() -> CompletableFuture.completedFuture("second"));

      assertEquals("second", second.get(1, TimeUnit.SECONDS));
      assertTrue(first.isCancelled());
    } finally {
      scheduler.shutdownNow();
    }
  }
}
