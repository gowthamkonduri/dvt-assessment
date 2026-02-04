package com.dvt.loyalty.service;

import com.dvt.loyalty.PointsQuoteResponse;
import com.dvt.loyalty.api.PointsQuoteRequest;
import com.dvt.loyalty.api.SupportedCurrencies;
import com.dvt.loyalty.api.Warnings;
import com.dvt.loyalty.client.FxClient;
import com.dvt.loyalty.client.PromoClient;
import com.dvt.loyalty.client.UpstreamException;
import com.dvt.loyalty.config.BusinessRules;
import com.dvt.loyalty.util.Retry;
import com.dvt.loyalty.util.Time;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Core business logic for calculating loyalty points from fare purchases.
 *
 * This service orchestrates calls to external FX and Promo services to compute
 * the final points breakdown. It implements the following resilience patterns:
 *
 * - Retry with backoff: FX service calls are retried on 5xx errors
 * - Graceful degradation: Promo service failures return 0 bonus (not an error)
 * - Fail-fast validation: Invalid requests are rejected before external calls
 * - Points cap: Total points are capped at {@link BusinessRules#TOTAL_POINTS_CAP}
 *
 * Thread Safety: This class is thread-safe and can be shared across multiple
 * request handlers.
 *
 * @see PointsQuoteRequest
 * @see PointsQuoteResponse
 * @see ValidationException
 */
public final class QuoteService {

  private static final Logger log = LoggerFactory.getLogger(QuoteService.class);

  private final FxClient fxClient;
  private final PromoClient promoClient;
  private final int fxMaxAttempts;

  /**
   * Creates a new QuoteService.
   *
   * @param fxClient      client for FX rate conversions
   * @param promoClient   client for promo code lookups
   * @param fxMaxAttempts max retry attempts for FX service (>= 1)
   */
  public QuoteService(FxClient fxClient, PromoClient promoClient, int fxMaxAttempts) {
    this.fxClient = Objects.requireNonNull(fxClient, "fxClient is required");
    this.promoClient = Objects.requireNonNull(promoClient, "promoClient is required");
    this.fxMaxAttempts = fxMaxAttempts;
  }

  /**
   * Calculates loyalty points for a fare purchase.
   *
   * Steps:
   * 1. Validates the request (fare amount, currency, cabin class, tier)
   * 2. Converts the fare to AED using the FX service
   * 3. Calculates base points (1 point per AED, truncated)
   * 4. Applies tier bonus percentage based on customer tier
   * 5. Optionally fetches promo bonus (gracefully degrades on failure)
   * 6. Caps total points at {@link BusinessRules#TOTAL_POINTS_CAP}
   *
   * Error Handling:
   * - {@link ValidationException} - Request validation failed (invalid input)
   * - {@link UpstreamException} - FX service unavailable after retries
   *
   * Warnings: The response may include:
   * - PROMO_EXPIRES_SOON: Promo code expires within 7 days
   * - PROMO_TIMEOUT: Promo service timed out (bonus set to 0)
   * - PROMO_UNAVAILABLE: Promo service returned an error (bonus set to 0)
   *
   * @param request the quote request containing fare details and customer information
   * @return a {@link Future} containing the points breakdown, FX rate, and any warnings
   * @throws ValidationException if request validation fails (returned as failed Future)
   * @throws UpstreamException if FX service is unavailable after all retry attempts
   */
  public Future<PointsQuoteResponse> quote(PointsQuoteRequest request) {
    // Fail fast if the request is garbage
    var validationErrors = validate(request);
    if (!validationErrors.isEmpty()) {
      log.debug("Validation failed: {}", validationErrors);
      return Future.failedFuture(new ValidationException(validationErrors));
    }

    log.debug("Processing quote: currency={}, fareAmount={}, tier={}",
      request.currency(), request.fareAmount(), request.customerTier());

    // FX service can be flaky, so we retry on server errors
    return Retry.withRetries(
        () -> fxClient.quote(request.currency(), request.fareAmount()),
        fxMaxAttempts,
        QuoteService::isFxRetriable
      )
      .compose(fx -> {
        log.debug("FX conversion complete: rate={}, convertedAmount={}", fx.rate(), fx.convertedAmount());
        
        // Base points = converted amount truncated to int (no rounding up)
        int basePoints = fx.convertedAmount().setScale(0, RoundingMode.DOWN).intValue();
        int tierBonus = (int) Math.floor(basePoints * request.customerTier().bonusRate());
        log.debug("Calculated points: basePoints={}, tierBonus={}", basePoints, tierBonus);

        var warnings = new ArrayList<String>();

        // Promo is optional - if missing or service fails, we just give 0 bonus (don't block the quote)
        Future<Integer> promoBonusFuture;
        if (request.promoCode() == null || request.promoCode().isBlank()) {
          log.debug("No promo code provided");
          promoBonusFuture = Future.succeededFuture(0);
        } else {
          log.debug("Looking up promo code: {}", request.promoCode());
          promoBonusFuture = promoClient.fetch(request.promoCode())
            .map(promo -> {
              maybeAddExpiryWarning(promo.expiresAt(), warnings);
              int bonus = Math.max(0, promo.bonusPoints()); // sanity check - no negative bonuses
              log.debug("Promo applied: code={}, bonusPoints={}", request.promoCode(), bonus);
              return bonus;
            })
            .recover(err -> {
              // Promo service down? No worries, just warn the user and move on
              log.warn("Promo lookup failed for code={}: {}", request.promoCode(), err.getMessage());
              warnings.add(isTimeout(err) ? Warnings.PROMO_TIMEOUT : Warnings.PROMO_UNAVAILABLE);
              return Future.succeededFuture(0);
            });
        }

        return promoBonusFuture.map(promoBonus -> {
          int total = basePoints + tierBonus + promoBonus;
          boolean capped = total > BusinessRules.TOTAL_POINTS_CAP;
          if (capped) {
            log.debug("Points capped: original={}, capped={}", total, BusinessRules.TOTAL_POINTS_CAP);
            total = BusinessRules.TOTAL_POINTS_CAP;
          }

          log.info("Quote completed: basePoints={}, tierBonus={}, promoBonus={}, total={}, capped={}",
            basePoints, tierBonus, promoBonus, total, capped);

          return new PointsQuoteResponse(
            basePoints,
            tierBonus,
            promoBonus,
            total,
            fx.rate(),
            List.copyOf(warnings)
          );
        });
      });
  }

  // Only retry on server errors (5xx). Client errors (4xx) mean we sent bad data, no point retrying.
  private static boolean isFxRetriable(Throwable err) {
    if (err instanceof UpstreamException ue) {
      return ue.status() >= 500;
    }
    return true; // network issues etc - worth a retry
  }

  private static List<String> validate(PointsQuoteRequest request) {
    var errors = new ArrayList<String>();

    if (request == null) {
      errors.add("Request body is required");
      return errors;
    }

    if (request.fareAmount() == null) {
      errors.add("fareAmount is required");
    } else if (request.fareAmount().signum() <= 0) {
      errors.add(String.format("fareAmount must be > 0 (received: %s)", request.fareAmount()));
    } else if (request.fareAmount().compareTo(BusinessRules.MIN_FARE_AMOUNT) < 0) {
      errors.add(String.format("fareAmount must be at least %s (received: %s)", 
        BusinessRules.MIN_FARE_AMOUNT, request.fareAmount()));
    } else if (request.fareAmount().compareTo(BusinessRules.MAX_FARE_AMOUNT) > 0) {
      errors.add(String.format("fareAmount must not exceed %s (received: %s)", 
        BusinessRules.MAX_FARE_AMOUNT, request.fareAmount()));
    }

    if (request.currency() == null || request.currency().isBlank()) {
      errors.add("currency is required");
    } else if (!SupportedCurrencies.VALUES.contains(request.currency())) {
      errors.add(String.format("currency must be one of: %s (received: %s)",
        String.join(", ", SupportedCurrencies.VALUES), request.currency()));
    }

    if (request.cabinClass() == null) {
      errors.add("cabinClass is required");
    }

    if (request.customerTier() == null) {
      errors.add("customerTier is required");
    }

    // Promo code is optional, but if provided must match expected format (security: prevents injection)
    if (request.promoCode() != null && !request.promoCode().isBlank()) {
      if (!BusinessRules.isValidPromoCode(request.promoCode())) {
        errors.add(String.format("promoCode must be uppercase alphanumeric (hyphens and underscores allowed), 1-%d characters (received: %s)",
          BusinessRules.PROMO_CODE_MAX_LENGTH, request.promoCode()));
      }
    }

    if (!errors.isEmpty()) {
      return errors;
    }

    return List.of();
  }

  private static void maybeAddExpiryWarning(String expiresAt, List<String> warnings) {
    if (expiresAt == null || expiresAt.isBlank()) {
      return;
    }
    try {
      Instant expiry = Instant.parse(expiresAt);
      if (Time.isWithinDays(expiry, Instant.now(), BusinessRules.PROMO_EXPIRY_WARNING_DAYS)) {
        warnings.add(Warnings.PROMO_EXPIRES_SOON);
      }
    } catch (Exception ignored) {
      // Ignore bad expiry formats; promo still applies.
    }
  }

  private static boolean isTimeout(Throwable err) {
    String name = err.getClass().getName();
    return name.contains("Timeout") || name.contains("timeout");
  }
}
