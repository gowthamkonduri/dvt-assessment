package com.dvt.loyalty.client;

public record PromoResponse(
  int bonusPoints,
  String expiresAt
) {
}
