package com.dvt.loyalty.util;

import java.time.Duration;
import java.time.Instant;

public final class Time {
  private Time() {}

  /**
   * Checks if an instant falls within the specified number of days from now.
   *
   * @param instant the instant to check (may be null)
   * @param now the reference time to compare against
   * @param days the number of days in the future to allow
   * @return true if instant is not null and is not after (now + days), false otherwise
   */
  public static boolean isWithinDays(Instant instant, Instant now, long days) {
    if (instant == null) {
      return false;
    }
    var limit = now.plus(Duration.ofDays(days));
    return !instant.isAfter(limit);
  }
}
