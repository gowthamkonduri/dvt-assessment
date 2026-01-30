package com.dvt.loyalty.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TimeTest {

  @Test
  void isWithinDays_whenInstantIsNull_returnsFalse() {
    assertThat(Time.isWithinDays(null, Instant.now(), 7)).isFalse();
  }

  @Test
  void isWithinDays_whenInstantBeforeLimit_returnsTrue() {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    Instant instant = now.plusSeconds(6 * 24L * 60L * 60L);
    assertThat(Time.isWithinDays(instant, now, 7)).isTrue();
  }

  @Test
  void isWithinDays_whenInstantAfterLimit_returnsFalse() {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    Instant instant = now.plusSeconds(8 * 24L * 60L * 60L);
    assertThat(Time.isWithinDays(instant, now, 7)).isFalse();
  }
}
