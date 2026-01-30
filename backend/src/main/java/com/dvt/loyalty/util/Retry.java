package com.dvt.loyalty.util;

import io.vertx.core.Future;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Simple retry helper for Vert.x Futures.
 * Keeps trying until max attempts or the error isn't worth retrying.
 */
public final class Retry {
  private Retry() {}

  /**
   * Retries the supplier up to maxAttempts times.
   * The isRetriable predicate decides if we should bother retrying a given error.
   */
  public static <T> Future<T> withRetries(
    Supplier<Future<T>> attempt,
    int maxAttempts,
    Predicate<Throwable> isRetriable
  ) {
    Objects.requireNonNull(attempt);
    Objects.requireNonNull(isRetriable);

    if (maxAttempts <= 1) {
      return attempt.get();
    }

    return attempt.get().recover(err -> {
      if (!isRetriable.test(err)) {
        return Future.failedFuture(err);
      }
      return withRetries(attempt, maxAttempts - 1, isRetriable);
    });
  }
}
