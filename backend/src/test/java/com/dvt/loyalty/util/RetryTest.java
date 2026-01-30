package com.dvt.loyalty.util;

import io.vertx.core.Future;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RetryTest {

  @Test
  void withRetries_whenMaxAttemptsIsOne_doesNotRetry() {
    var calls = new AtomicInteger();

    var result = Retry.withRetries(
      () -> {
        calls.incrementAndGet();
        return Future.succeededFuture("ok");
      },
      1,
      err -> true
    );

    assertThat(result.succeeded()).isTrue();
    assertThat(result.result()).isEqualTo("ok");
    assertThat(calls.get()).isEqualTo(1);
  }

  @Test
  void withRetries_whenNotRetriable_failsWithoutRetry() {
    var calls = new AtomicInteger();

    var result = Retry.withRetries(
      () -> {
        calls.incrementAndGet();
        return Future.failedFuture(new RuntimeException("boom"));
      },
      3,
      err -> false
    );

    assertThat(result.failed()).isTrue();
    assertThat(calls.get()).isEqualTo(1);
  }
}
