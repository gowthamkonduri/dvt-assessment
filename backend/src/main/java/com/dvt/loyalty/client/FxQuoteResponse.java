package com.dvt.loyalty.client;

import java.math.BigDecimal;

public record FxQuoteResponse(
  BigDecimal convertedAmount,
  double rate
) {
}
