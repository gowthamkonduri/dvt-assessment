package com.dvt.loyalty.api;

import java.util.Set;

public final class SupportedCurrencies {
  private SupportedCurrencies() {}

  // Assumption: keep it small and explicit for the assignment.
  public static final Set<String> VALUES = Set.of(
    "USD",
    "EUR",
    "GBP",
    "AUD"
  );
}
