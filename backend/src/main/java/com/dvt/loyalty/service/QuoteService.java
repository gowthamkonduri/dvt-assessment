package com.dvt.loyalty.service;

import com.dvt.loyalty.PointsQuoteResponse;
import com.dvt.loyalty.api.PointsQuoteRequest;
import com.dvt.loyalty.api.SupportedCurrencies;
import com.dvt.loyalty.api.Warnings;
import com.dvt.loyalty.client.FxClient;
import com.dvt.loyalty.client.PromoClient;
import com.dvt.loyalty.client.UpstreamException;
import com.dvt.loyalty.util.Retry;
import com.dvt.loyalty.util.Time;
import io.vertx.core.Future;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Core business logic for calculating loyalty points.
 * Orchestrates FX conversion, tier bonuses, and promo lookups.
 */
public final class QuoteService {

  // Business rule: no one gets more than 50k points per booking
  private static final int TOTAL_CAP = 50_000;

  private final FxClient fxClient;
  private final PromoClient promoClient;
  private final int fxMaxAttempts;

  public QuoteService(FxClient fxClient, PromoClient promoClient, int fxMaxAttempts) {
    this.fxClient = Objects.requireNonNull(fxClient);
    this.promoClient = Objects.requireNonNull(promoClient);
    this.fxMaxAttempts = fxMaxAttempts;
  }

  /**
   * Main entry point - validates input, fetches FX rate, applies tier/promo bonuses.
   * FX call is retried on 5xx errors; promo failures are gracefully handled (we just skip the bonus).
   */
  public Future<PointsQuoteResponse> quote(PointsQuoteRequest request) {
    // Fail fast if the request is garbage
    var validationErrors = validate(request);
    if (!validationErrors.isEmpty()) {
      return Future.failedFuture(new ValidationException(validationErrors));
    }

    // FX service can be flaky, so we retry on server errors
    return Retry.withRetries(
        () -> fxClient.quote(request.currency(), request.fareAmount()),
        fxMaxAttempts,
        QuoteService::isFxRetriable
      )
      .compose(fx -> {
        // Base points = converted amount truncated to int (no rounding up)
        int basePoints = fx.convertedAmount().setScale(0, RoundingMode.DOWN).intValue();
        int tierBonus = (int) Math.floor(basePoints * request.customerTier().bonusRate());

        var warnings = new ArrayList<String>();

        // Promo is optional - if missing or service fails, we just give 0 bonus (don't block the quote)
        Future<Integer> promoBonusFuture;
        if (request.promoCode() == null || request.promoCode().isBlank()) {
          promoBonusFuture = Future.succeededFuture(0);
        } else {
          promoBonusFuture = promoClient.fetch(request.promoCode())
            .map(promo -> {
              maybeAddExpiryWarning(promo.expiresAt(), warnings);
              return Math.max(0, promo.bonusPoints()); // sanity check - no negative bonuses
            })
            .recover(err -> {
              // Promo service down? No worries, just warn the user and move on
              warnings.add(isTimeout(err) ? Warnings.PROMO_TIMEOUT : Warnings.PROMO_UNAVAILABLE);
              return Future.succeededFuture(0);
            });
        }

        return promoBonusFuture.map(promoBonus -> {
          int total = basePoints + tierBonus + promoBonus;
          if (total > TOTAL_CAP) {
            total = TOTAL_CAP;
          }

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
      errors.add("fareAmount must be > 0");
    }

    if (request.currency() == null || request.currency().isBlank()) {
      errors.add("currency is required");
    } else if (!SupportedCurrencies.VALUES.contains(request.currency())) {
      errors.add("currency must be one of " + SupportedCurrencies.VALUES);
    }

    if (request.cabinClass() == null) {
      errors.add("cabinClass is required");
    }

    if (request.customerTier() == null) {
      errors.add("customerTier is required");
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
      if (Time.isWithinDays(expiry, Instant.now(), 7)) {
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
