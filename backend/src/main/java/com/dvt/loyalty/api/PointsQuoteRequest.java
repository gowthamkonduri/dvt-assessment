package com.dvt.loyalty.api;

import java.math.BigDecimal;

/**
 * Request payload for calculating loyalty points from a fare purchase.
 *
 * All fields except promoCode are required.
 *
 * Validation Rules:
 * - fareAmount: Must be positive and within allowed range
 * - currency: Must be a supported ISO 4217 currency code
 * - cabinClass: Must be a valid cabin class enum value
 * - customerTier: Must be a valid customer tier enum value
 * - promoCode: Optional; if provided, must be alphanumeric with hyphens/underscores
 *
 * @param fareAmount   the fare amount in the specified currency (required, must be > 0)
 * @param currency     the ISO 4217 currency code (required, e.g., "USD", "EUR", "GBP")
 * @param cabinClass   the cabin class for the flight (required)
 * @param customerTier the customer's loyalty tier (required)
 * @param promoCode    optional promotional code for bonus points (nullable)
 */
public record PointsQuoteRequest(
  BigDecimal fareAmount,
  String currency,
  CabinClass cabinClass,
  CustomerTier customerTier,
  String promoCode
) {
}
