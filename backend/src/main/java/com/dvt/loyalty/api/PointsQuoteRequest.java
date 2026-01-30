package com.dvt.loyalty.api;

import java.math.BigDecimal;

public record PointsQuoteRequest(
  BigDecimal fareAmount,
  String currency,
  CabinClass cabinClass,
  CustomerTier customerTier,
  String promoCode
) {
}
