package com.dvt.loyalty.client;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.client.WebClient;

import java.util.Objects;

/**
 * Fetches promo details from external promo service.
 * Has a short timeout since promos are nice-to-have - we don't want to block the quote.
 */
public final class PromoClient {

  private final WebClient webClient;
  private final String baseUrl;
  private final long timeoutMs;

  public PromoClient(Vertx vertx, String baseUrl, long timeoutMs) {
    this.webClient = WebClient.create(vertx);
    this.baseUrl = Objects.requireNonNull(baseUrl);
    this.timeoutMs = timeoutMs;
  }

  public Future<PromoResponse> fetch(String promoCode) {
    String url = baseUrl + "/promo/" + promoCode;

    return webClient
      .getAbs(url)
      .timeout(timeoutMs)
      .send()
      .compose(resp -> {
        if (resp.statusCode() != 200) {
          return Future.failedFuture(new UpstreamException("PROMO", resp.statusCode(), "Promo service error"));
        }
        try {
          var parsed = Json.decodeValue(resp.body(), PromoResponse.class);
          if (parsed == null) {
            return Future.failedFuture(new UpstreamException("PROMO", 502, "Promo response missing fields"));
          }
          return Future.succeededFuture(parsed);
        } catch (Exception e) {
          return Future.failedFuture(new UpstreamException("PROMO", 502, "Promo response invalid JSON"));
        }
      });
  }
}
