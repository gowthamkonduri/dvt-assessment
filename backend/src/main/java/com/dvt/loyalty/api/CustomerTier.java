package com.dvt.loyalty.api;

public enum CustomerTier {
  NONE(0.00),
  SILVER(0.15),
  GOLD(0.30),
  PLATINUM(0.50);

  private final double bonusRate;

  CustomerTier(double bonusRate) {
    this.bonusRate = bonusRate;
  }

  public double bonusRate() {
    return bonusRate;
  }
}
