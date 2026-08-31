package net.dyhntastic.radio.core.util;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class SearchDebouncer {

  private final ScheduledExecutorService scheduler;
  private final Duration delay;
  private final AtomicReference<Pending> pending = new AtomicReference<>();
  private final AtomicLong generation = new AtomicLong();

  public SearchDebouncer(ScheduledExecutorService scheduler, Duration delay) {
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    this.delay = Objects.requireNonNull(delay, "delay");
  }

  public <T> CompletableFuture<T> submit(Supplier<CompletableFuture<T>> action) {
    long currentGeneration = this.generation.incrementAndGet();
    CompletableFuture<T> result = new CompletableFuture<>();
    ScheduledFuture<?> scheduled = this.scheduler.schedule(() -> {
      if (this.generation.get() != currentGeneration) {
        result.cancel(false);
        return;
      }
      try {
        action.get().whenComplete((value, error) -> {
          if (error == null) {
            result.complete(value);
          } else {
            result.completeExceptionally(error);
          }
        });
      } catch (RuntimeException exception) {
        result.completeExceptionally(exception);
      }
    }, this.delay.toMillis(), TimeUnit.MILLISECONDS);
    Pending previous = this.pending.getAndSet(new Pending(scheduled, result));
    if (previous != null) {
      previous.scheduled().cancel(false);
      previous.result().cancel(false);
    }
    return result;
  }

  public void cancel() {
    this.generation.incrementAndGet();
    Pending value = this.pending.getAndSet(null);
    if (value != null) {
      value.scheduled().cancel(false);
      value.result().cancel(false);
    }
  }

  private record Pending(ScheduledFuture<?> scheduled, CompletableFuture<?> result) {
  }
}
