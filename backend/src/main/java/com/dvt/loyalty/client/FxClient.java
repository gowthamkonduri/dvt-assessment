package com.dvt.loyalty.client;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * HTTP client for the Foreign Exchange (FX) rate service.
 *
 * Converts fare amounts from various currencies to AED using real-time exchange rates.
 * Uses connection pooling with keep-alive. Thread-safe.
 *
 * @see FxQuoteResponse
 * @see UpstreamException
 */
public final class FxClient implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(FxClient.class);

  private final WebClient webClient;
  private final String baseUrl;
  private final long timeoutMs;

  /**
   * Creates a new FxClient.
   *
   * @param vertx     the Vert.x instance
   * @param baseUrl   base URL of the FX service
   * @param timeoutMs request timeout in milliseconds
   */
  public FxClient(Vertx vertx, String baseUrl, long timeoutMs) {
    WebClientOptions options = new WebClientOptions()
      .setMaxPoolSize(10)
      .setKeepAlive(true)
      .setConnectTimeout(5000)
      .setIdleTimeout(30);
    this.webClient = WebClient.create(vertx, options);
    this.baseUrl = Objects.requireNonNull(baseUrl);
    this.timeoutMs = timeoutMs;
    log.debug("FxClient initialized with baseUrl={}, timeoutMs={}, poolSize={}", baseUrl, timeoutMs, options.getMaxPoolSize());
  }

  /** Closes the underlying HTTP client. */
  @Override
  public void close() {
    if (webClient != null) {
      webClient.close();
      log.debug("FxClient WebClient closed");
    }
  }

  /**
   * Fetches the exchange rate and converts an amount to AED.
   *
   * @param currency the source currency code (e.g., "USD", "EUR", "GBP")
   * @param amount   the amount to convert
   * @return a Future containing the converted amount and exchange rate
   * @throws UpstreamException if the FX service returns non-200 or invalid response
   */
  public Future<FxQuoteResponse> quote(String currency, BigDecimal amount) {
    String url = baseUrl + "/fx/quote";
    log.debug("Requesting FX quote: currency={}, amount={}", currency, amount);

    return webClient
      .getAbs(url)
      .addQueryParam("currency", currency)
      .addQueryParam("amount", amount.toPlainString())
      .timeout(timeoutMs)
      .send()
      .compose(resp -> {
        if (resp.statusCode() != 200) {
          log.warn("FX service returned error: status={}", resp.statusCode());
          return Future.failedFuture(new UpstreamException("FX", resp.statusCode(), "FX service error"));
        }
        try {
          var parsed = Json.decodeValue(resp.body(), FxQuoteResponse.class);
          if (parsed == null || parsed.convertedAmount() == null) {
            log.warn("FX response missing required fields");
            return Future.failedFuture(new UpstreamException("FX", 502, "FX response missing fields"));
          }
          log.debug("FX quote received: rate={}, convertedAmount={}", parsed.rate(), parsed.convertedAmount());
          return Future.succeededFuture(parsed);
        } catch (DecodeException e) {
          log.error("Failed to parse FX response", e);
          return Future.failedFuture(new UpstreamException("FX", 502, "FX response invalid JSON: " + e.getMessage()));
        }
      });
  }
}
