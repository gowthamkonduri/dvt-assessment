package com.dvt.loyalty.util;

import java.time.Instant;

public final class Time {
  private Time() {}

  public static boolean isWithinDays(Instant instant, Instant now, long days) {
    if (instant == null) {
      return false;
    }
    var limit = now.plusSeconds(days * 24L * 60L * 60L);
    return !instant.isAfter(limit);
  }
}
