package com.dvt.loyalty;

import java.util.List;

public record PointsQuoteResponse(
  int basePoints,
  int tierBonus,
  int promoBonus,
  int totalPoints,
  double effectiveFxRate,
  List<String> warnings
) {
}
