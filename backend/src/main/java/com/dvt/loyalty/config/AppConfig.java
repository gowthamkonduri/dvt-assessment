package com.dvt.loyalty.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Centralized application configuration.
 * Reads from environment variables with sensible defaults.
 */
public final class AppConfig {

  private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

  private final int port;
  private final String fxBaseUrl;
  private final String promoBaseUrl;
  private final long fxTimeoutMs;
  private final long promoTimeoutMs;
  private final int fxMaxAttempts;

  public AppConfig(
    int port,
    String fxBaseUrl,
    String promoBaseUrl,
    long fxTimeoutMs,
    long promoTimeoutMs,
    int fxMaxAttempts
  ) {
    this.port = port;
    this.fxBaseUrl = Objects.requireNonNull(fxBaseUrl, "fxBaseUrl is required");
    this.promoBaseUrl = Objects.requireNonNull(promoBaseUrl, "promoBaseUrl is required");
    this.fxTimeoutMs = fxTimeoutMs;
    this.promoTimeoutMs = promoTimeoutMs;
    this.fxMaxAttempts = fxMaxAttempts;
  }

  /**
   * Creates configuration from environment variables with sensible defaults.
   */
  public static AppConfig fromEnvironment() {
    return new AppConfig(
      parseIntOrDefault("PORT", 8080),
      getOrDefault("FX_BASE_URL", "http://localhost:8081"),
      getOrDefault("PROMO_BASE_URL", "http://localhost:8082"),
      parseLongOrDefault("FX_TIMEOUT_MS", 500L),
      parseLongOrDefault("PROMO_TIMEOUT_MS", 300L),
      parseIntOrDefault("FX_MAX_ATTEMPTS", BusinessRules.DEFAULT_FX_MAX_ATTEMPTS)
    );
  }

  private static String getOrDefault(String key, String defaultValue) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      log.debug("Config {} not set, using default: {}", key, defaultValue);
      return defaultValue;
    }
    log.debug("Config {} = {}", key, value);
    return value;
  }

  private static int parseIntOrDefault(String key, int defaultValue) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      log.debug("Config {} not set, using default: {}", key, defaultValue);
      return defaultValue;
    }
    try {
      int parsed = Integer.parseInt(value);
      log.debug("Config {} = {}", key, parsed);
      return parsed;
    } catch (NumberFormatException e) {
      log.warn("Invalid integer value for {}: '{}', using default: {}", key, value, defaultValue);
      return defaultValue;
    }
  }

  private static long parseLongOrDefault(String key, long defaultValue) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      log.debug("Config {} not set, using default: {}", key, defaultValue);
      return defaultValue;
    }
    try {
      long parsed = Long.parseLong(value);
      log.debug("Config {} = {}", key, parsed);
      return parsed;
    } catch (NumberFormatException e) {
      log.warn("Invalid long value for {}: '{}', using default: {}", key, value, defaultValue);
      return defaultValue;
    }
  }

  // Getters

  public int port() {
    return port;
  }

  public String fxBaseUrl() {
    return fxBaseUrl;
  }

  public String promoBaseUrl() {
    return promoBaseUrl;
  }

  public long fxTimeoutMs() {
    return fxTimeoutMs;
  }

  public long promoTimeoutMs() {
    return promoTimeoutMs;
  }

  public int fxMaxAttempts() {
    return fxMaxAttempts;
  }

  /**
   * Returns a new AppConfig with just the port changed.
   * Useful for tests that need a specific port but want other defaults.
   */
  public AppConfig withPort(int port) {
    return new AppConfig(port, fxBaseUrl, promoBaseUrl, fxTimeoutMs, promoTimeoutMs, fxMaxAttempts);
  }

  @Override
  public String toString() {
    return "AppConfig{" +
      "port=" + port +
      ", fxBaseUrl='" + fxBaseUrl + '\'' +
      ", promoBaseUrl='" + promoBaseUrl + '\'' +
      ", fxTimeoutMs=" + fxTimeoutMs +
      ", promoTimeoutMs=" + promoTimeoutMs +
      ", fxMaxAttempts=" + fxMaxAttempts +
      '}';
  }
}
