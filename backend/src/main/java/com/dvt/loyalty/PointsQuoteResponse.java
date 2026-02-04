package com.dvt.loyalty;

import java.util.List;

/**
 * Response payload containing the calculated loyalty points breakdown.
 *
 * Points Calculation: totalPoints = min(basePoints + tierBonus + promoBonus, 50000)
 *
 * @param basePoints      points earned from the fare amount (1 point per AED, truncated)
 * @param tierBonus       additional points from customer tier (percentage of base points)
 * @param promoBonus      bonus points from promotional code (0 if no promo or promo failed)
 * @param totalPoints     sum of all points, capped at 50,000
 * @param effectiveFxRate the exchange rate used for currency conversion
 * @param warnings        list of warning codes (e.g., "PROMO_EXPIRES_SOON", "PROMO_TIMEOUT")
 */
public record PointsQuoteResponse(
  int basePoints,
  int tierBonus,
  int promoBonus,
  int totalPoints,
  double effectiveFxRate,
  List<String> warnings
) {
}
