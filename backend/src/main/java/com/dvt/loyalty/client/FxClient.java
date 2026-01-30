package com.dvt.loyalty.client;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.client.WebClient;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Calls the external FX service to convert fare amounts.
 * In tests, we stub this with WireMock.
 */
public final class FxClient {

  private final WebClient webClient;
  private final String baseUrl;
  private final long timeoutMs;

  public FxClient(Vertx vertx, String baseUrl, long timeoutMs) {
    this.webClient = WebClient.create(vertx);
    this.baseUrl = Objects.requireNonNull(baseUrl);
    this.timeoutMs = timeoutMs;
  }

  public Future<FxQuoteResponse> quote(String currency, BigDecimal amount) {
    String url = baseUrl + "/fx/quote";

    return webClient
      .getAbs(url)
      .addQueryParam("currency", currency)
      .addQueryParam("amount", amount.toPlainString())
      .timeout(timeoutMs)
      .send()
      .compose(resp -> {
        if (resp.statusCode() != 200) {
          return Future.failedFuture(new UpstreamException("FX", resp.statusCode(), "FX service error"));
        }
        try {
          var parsed = Json.decodeValue(resp.body(), FxQuoteResponse.class);
          if (parsed == null || parsed.convertedAmount() == null) {
            return Future.failedFuture(new UpstreamException("FX", 502, "FX response missing fields"));
          }
          return Future.succeededFuture(parsed);
        } catch (Exception e) {
          return Future.failedFuture(new UpstreamException("FX", 502, "FX response invalid JSON"));
        }
      });
  }
}
