package com.dvt.loyalty.client;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * HTTP client for the promo service.
 * Uses short timeout since promo is optional - failures return empty result.
 */
public final class PromoClient implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(PromoClient.class);

  private final WebClient webClient;
  private final String baseUrl;
  private final long timeoutMs;

  public PromoClient(Vertx vertx, String baseUrl, long timeoutMs) {
    WebClientOptions options = new WebClientOptions()
      .setMaxPoolSize(10)
      .setKeepAlive(true)
      .setConnectTimeout(5000)
      .setIdleTimeout(30);
    this.webClient = WebClient.create(vertx, options);
    this.baseUrl = Objects.requireNonNull(baseUrl);
    this.timeoutMs = timeoutMs;
    log.debug("PromoClient initialized with baseUrl={}, timeoutMs={}, poolSize={}", baseUrl, timeoutMs, options.getMaxPoolSize());
  }

  @Override
  public void close() {
    if (webClient != null) {
      webClient.close();
      log.debug("PromoClient WebClient closed");
    }
  }

  /** Fetches bonus points for a promo code. */
  public Future<PromoResponse> fetch(String promoCode) {
    String url = baseUrl + "/promo/" + promoCode;
    log.debug("Fetching promo: code={}", promoCode);

    return webClient
      .getAbs(url)
      .timeout(timeoutMs)
      .send()
      .compose(resp -> {
        if (resp.statusCode() != 200) {
          log.warn("Promo service returned error: code={}, status={}", promoCode, resp.statusCode());
          return Future.failedFuture(new UpstreamException("PROMO", resp.statusCode(), "Promo service error"));
        }
        try {
          var parsed = Json.decodeValue(resp.body(), PromoResponse.class);
          if (parsed == null) {
            log.warn("Promo response missing required fields: code={}", promoCode);
            return Future.failedFuture(new UpstreamException("PROMO", 502, "Promo response missing fields"));
          }
          log.debug("Promo fetched successfully: code={}, bonusPoints={}", promoCode, parsed.bonusPoints());
          return Future.succeededFuture(parsed);
        } catch (DecodeException e) {
          log.error("Failed to parse Promo response: code={}", promoCode, e);
          return Future.failedFuture(new UpstreamException("PROMO", 502, "Promo response invalid JSON: " + e.getMessage()));
        }
      });
  }
}
