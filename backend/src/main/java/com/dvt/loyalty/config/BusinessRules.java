package com.dvt.loyalty.config;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Centralized business rules and validation constants.
 * All magic numbers and business constraints should be defined here.
 */
public final class BusinessRules {

  private BusinessRules() {
    // Utility class - no instantiation
  }

  // ==================== Points Calculation ====================

  /**
   * Maximum total points that can be awarded per booking.
   * Prevents abuse and ensures sustainable rewards program.
   */
  public static final int TOTAL_POINTS_CAP = 50_000;

  // ==================== Fare Amount Limits ====================

  /**
   * Minimum fare amount (in any currency) that qualifies for points.
   * Prevents micro-transactions from gaming the system.
   */
  public static final BigDecimal MIN_FARE_AMOUNT = new BigDecimal("0.01");

  /**
   * Maximum fare amount (in any currency) that can be processed.
   * Prevents data entry errors and potential overflow issues.
   */
  public static final BigDecimal MAX_FARE_AMOUNT = new BigDecimal("1000000.00");

  // ==================== Promo Code Rules ====================

  /**
   * Maximum length for promo codes.
   * Kept intentionally short to prevent injection attempts and simplify validation.
   */
  public static final int PROMO_CODE_MAX_LENGTH = 20;

  /**
   * Pattern for valid promo codes: uppercase alphanumeric with optional hyphens/underscores.
   * This strict pattern prevents injection attacks and ensures consistent formatting.
   * Examples: SUMMER25, FIRST-FLIGHT, VIP_2025
   */
  public static final Pattern PROMO_CODE_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9_-]*$");

  /**
   * Number of days before expiry to show a warning about the promo code.
   */
  public static final int PROMO_EXPIRY_WARNING_DAYS = 7;

  // ==================== Retry Configuration ====================

  /**
   * Default maximum retry attempts for FX service calls.
   */
  public static final int DEFAULT_FX_MAX_ATTEMPTS = 3;

  // ==================== Validation Helpers ====================

  /**
   * Validates if a fare amount is within acceptable bounds.
   *
   * @param fareAmount the fare amount to validate
   * @return true if the amount is valid, false otherwise
   */
  public static boolean isValidFareAmount(BigDecimal fareAmount) {
    if (fareAmount == null) {
      return false;
    }
    return fareAmount.compareTo(MIN_FARE_AMOUNT) >= 0
        && fareAmount.compareTo(MAX_FARE_AMOUNT) <= 0;
  }

  /**
   * Validates if a promo code matches the expected format.
   *
   * @param promoCode the promo code to validate (can be null or blank)
   * @return true if the promo code is valid or not provided, false if provided but invalid
   */
  public static boolean isValidPromoCode(String promoCode) {
    if (promoCode == null || promoCode.isBlank()) {
      return true; // Promo code is optional
    }
    if (promoCode.length() > PROMO_CODE_MAX_LENGTH) {
      return false;
    }
    return PROMO_CODE_PATTERN.matcher(promoCode).matches();
  }
}
