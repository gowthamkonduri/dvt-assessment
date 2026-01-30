package com.dvt.loyalty;

import com.dvt.loyalty.api.Warnings;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@ExtendWith(VertxExtension.class)
class QuoteVerticleComponentTest {

  private WireMockServer fx;
  private WireMockServer promo;

  @BeforeEach
  void startWireMock() {
    fx = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    promo = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    fx.start();
    promo.start();
  }

  @AfterEach
  void stopWireMock() {
    if (promo != null) {
      promo.stop();
    }
    if (fx != null) {
      fx.stop();
    }
  }

  @Test
  void happyPath_returnsExpectedPoints_andWarning(Vertx vertx, VertxTestContext tc) {
    fx.stubFor(get(urlPathEqualTo("/fx/quote"))
      .withQueryParam("currency", equalTo("USD"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody("{" +
          "\"convertedAmount\":1234.50," +
          "\"rate\":3.67" +
          "}")));

    String expiresSoon = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
    promo.stubFor(get(urlEqualTo("/promo/SUMMER25"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody("{" +
          "\"bonusPoints\":308," +
          "\"expiresAt\":\"" + expiresSoon + "\"" +
          "}")));

    deployApp(vertx, 500, 300, 3)
      .compose(port -> sendQuoteRequest(vertx, port, requestBody(1234.50, "USD", "ECONOMY", "SILVER", "SUMMER25"), null))
      .onSuccess(resp -> tc.verify(() -> {
        assertThat(resp.statusCode()).isEqualTo(200);

        var json = resp.bodyAsJsonObject();
        assertThat(json.getInteger("basePoints")).isEqualTo(1234);
        assertThat(json.getInteger("tierBonus")).isEqualTo(185);
        assertThat(json.getInteger("promoBonus")).isEqualTo(308);
        assertThat(json.getInteger("totalPoints")).isEqualTo(1727);
        assertThat(json.getDouble("effectiveFxRate")).isEqualTo(3.67);
        assertThat(stringList(json, "warnings")).contains(Warnings.PROMO_EXPIRES_SOON);
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  @Test
  void fxRetries_on5xx_thenSucceeds(Vertx vertx, VertxTestContext tc) {
    fx.stubFor(get(urlPathEqualTo("/fx/quote"))
      .inScenario("fx-retry")
      .whenScenarioStateIs(Scenario.STARTED)
      .willReturn(aResponse().withStatus(500))
      .willSetStateTo("second"));

    fx.stubFor(get(urlPathEqualTo("/fx/quote"))
      .inScenario("fx-retry")
      .whenScenarioStateIs("second")
      .willReturn(aResponse().withStatus(502))
      .willSetStateTo("third"));

    fx.stubFor(get(urlPathEqualTo("/fx/quote"))
      .inScenario("fx-retry")
      .whenScenarioStateIs("third")
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody("{" +
          "\"convertedAmount\":100.00," +
          "\"rate\":1.0" +
          "}")));

    promo.stubFor(get(anyUrl())
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody("{" +
          "\"bonusPoints\":0," +
          "\"expiresAt\":\"" + Instant.now().plus(30, ChronoUnit.DAYS).toString() + "\"" +
          "}")));

    deployApp(vertx, 500, 300, 3)
      .compose(port -> sendQuoteRequest(vertx, port, requestBody(100.00, "USD", "ECONOMY", "NONE", "SUMMER25"), null))
      .onSuccess(resp -> tc.verify(() -> {
        assertThat(resp.statusCode()).isEqualTo(200);
        fx.verify(3, getRequestedFor(urlPathEqualTo("/fx/quote")));
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  @Test
  void promoTimeout_fallsBackToZero_andAddsWarning(Vertx vertx, VertxTestContext tc) {
    fx.stubFor(get(urlPathEqualTo("/fx/quote"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody("{" +
          "\"convertedAmount\":100.00," +
          "\"rate\":1.0" +
          "}")));

    promo.stubFor(get(urlEqualTo("/promo/SUMMER25"))
      .willReturn(aResponse()
        .withFixedDelay(500)
        .withHeader("Content-Type", "application/json")
        .withBody("{" +
          "\"bonusPoints\":999," +
          "\"expiresAt\":\"" + Instant.now().plus(30, ChronoUnit.DAYS).toString() + "\"" +
          "}")));

    deployApp(vertx, 500, 100, 3)
      .compose(port -> sendQuoteRequest(vertx, port, requestBody(100.00, "USD", "ECONOMY", "NONE", "SUMMER25"), null))
      .onSuccess(resp -> tc.verify(() -> {
        var json = resp.bodyAsJsonObject();
        assertThat(json.getInteger("promoBonus")).isEqualTo(0);
        assertThat(stringList(json, "warnings")).contains(Warnings.PROMO_TIMEOUT);
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  @Test
  void totalPoints_isCappedAt50000(Vertx vertx, VertxTestContext tc) {
    fx.stubFor(get(urlPathEqualTo("/fx/quote"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody("{" +
          "\"convertedAmount\":60000.00," +
          "\"rate\":1.0" +
          "}")));

    promo.stubFor(get(urlEqualTo("/promo/SUMMER25"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody("{" +
          "\"bonusPoints\":1000," +
          "\"expiresAt\":\"" + Instant.now().plus(30, ChronoUnit.DAYS).toString() + "\"" +
          "}")));

    deployApp(vertx, 500, 300, 3)
      .compose(port -> sendQuoteRequest(vertx, port, requestBody(60000.00, "USD", "BUSINESS", "PLATINUM", "SUMMER25"), null))
      .onSuccess(resp -> tc.verify(() -> {
        var json = resp.bodyAsJsonObject();
        assertThat(json.getInteger("totalPoints")).isEqualTo(50_000);
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  @Test
  void validation_rejectsNonPositiveFare_andDoesNotCallUpstreams(Vertx vertx, VertxTestContext tc) {
    deployApp(vertx, 500, 300, 3)
      .compose(port -> sendQuoteRequest(vertx, port, requestBody(0.00, "USD", "ECONOMY", "NONE", "SUMMER25"), null))
      .onSuccess(resp -> tc.verify(() -> {
        assertThat(resp.statusCode()).isEqualTo(400);
        var json = resp.bodyAsJsonObject();
        assertThat(json.getString("code")).isEqualTo("VALIDATION_ERROR");
        fx.verify(0, getRequestedFor(urlPathEqualTo("/fx/quote")));
        promo.verify(0, getRequestedFor(urlMatching("/promo/.*")));
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  @Test
  void validation_rejectsInvalidCurrency(Vertx vertx, VertxTestContext tc) {
    deployApp(vertx, 500, 300, 3)
      .compose(port -> sendQuoteRequest(vertx, port, requestBody(10.00, "XXX", "ECONOMY", "NONE", ""), null))
      .onSuccess(resp -> tc.verify(() -> {
        assertThat(resp.statusCode()).isEqualTo(400);
        var json = resp.bodyAsJsonObject();
        assertThat(json.getString("code")).isEqualTo("VALIDATION_ERROR");
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  @Test
  void fxFailureAfterRetries_returns502(Vertx vertx, VertxTestContext tc) {
    fx.stubFor(get(urlPathEqualTo("/fx/quote")).willReturn(aResponse().withStatus(500)));

    deployApp(vertx, 200, 100, 3)
      .compose(port -> sendQuoteRequest(vertx, port, requestBody(10.00, "USD", "ECONOMY", "NONE", ""), null))
      .onSuccess(resp -> tc.verify(() -> {
        assertThat(resp.statusCode()).isEqualTo(502);
        var json = resp.bodyAsJsonObject();
        assertThat(json.getString("code")).isEqualTo("UPSTREAM_ERROR");
        assertThat(stringList(json, "details")).contains("FX");
        fx.verify(3, getRequestedFor(urlPathEqualTo("/fx/quote")));
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  @Test
  void invalidJson_returns400(Vertx vertx, VertxTestContext tc) {
    deployApp(vertx, 500, 300, 3)
      .compose(port -> {
        var client = WebClient.create(vertx);
        return client.post(port, "localhost", "/v1/points/quote")
          .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
          .sendBuffer(Buffer.buffer("{not valid json"));
      })
      .onSuccess(resp -> tc.verify(() -> {
        assertThat(resp.statusCode()).isEqualTo(400);
        var json = resp.bodyAsJsonObject();
        assertThat(json.getString("code")).isEqualTo("INVALID_JSON");
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  @Test
  void requestId_isEchoedWhenProvided(Vertx vertx, VertxTestContext tc) {
    fx.stubFor(get(urlPathEqualTo("/fx/quote"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody("{" +
          "\"convertedAmount\":10.00," +
          "\"rate\":1.0" +
          "}")));

    deployApp(vertx, 500, 300, 1)
      .compose(port -> sendQuoteRequest(vertx, port, requestBody(10.00, "USD", "ECONOMY", "NONE", null), "req-123"))
      .onSuccess(resp -> tc.verify(() -> {
        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.getHeader("x-request-id")).isEqualTo("req-123");
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  @Test
  void blankRequestId_isReplacedWithGeneratedId(Vertx vertx, VertxTestContext tc) {
    fx.stubFor(get(urlPathEqualTo("/fx/quote"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody("{" +
          "\"convertedAmount\":10.00," +
          "\"rate\":1.0" +
          "}")));

    deployApp(vertx, 500, 300, 1)
      .compose(port -> sendQuoteRequest(vertx, port, requestBody(10.00, "USD", "ECONOMY", "NONE", null), "   "))
      .onSuccess(resp -> tc.verify(() -> {
        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.getHeader("x-request-id")).isNotBlank();
        assertThat(resp.getHeader("x-request-id")).isNotEqualTo("   ");
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  @Test
  void fx4xx_isNotRetried_andReturns502(Vertx vertx, VertxTestContext tc) {
    fx.stubFor(get(urlPathEqualTo("/fx/quote"))
      .willReturn(aResponse().withStatus(400)));

    deployApp(vertx, 500, 300, 3)
      .compose(port -> sendQuoteRequest(vertx, port, requestBody(10.00, "USD", "ECONOMY", "NONE", null), null))
      .onSuccess(resp -> tc.verify(() -> {
        assertThat(resp.statusCode()).isEqualTo(502);
        fx.verify(1, getRequestedFor(urlPathEqualTo("/fx/quote")));
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  @Test
  void promoUnavailable_fallsBackToZero_andAddsWarning(Vertx vertx, VertxTestContext tc) {
    fx.stubFor(get(urlPathEqualTo("/fx/quote"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody("{" +
          "\"convertedAmount\":100.00," +
          "\"rate\":1.0" +
          "}")));

    promo.stubFor(get(urlEqualTo("/promo/SUMMER25"))
      .willReturn(aResponse().withStatus(500)));

    deployApp(vertx, 500, 300, 1)
      .compose(port -> sendQuoteRequest(vertx, port, requestBody(100.00, "USD", "ECONOMY", "NONE", "SUMMER25"), null))
      .onSuccess(resp -> tc.verify(() -> {
        assertThat(resp.statusCode()).isEqualTo(200);
        var json = resp.bodyAsJsonObject();
        assertThat(json.getInteger("promoBonus")).isEqualTo(0);
        assertThat(stringList(json, "warnings")).contains(Warnings.PROMO_UNAVAILABLE);
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  @Test
  void fxUnreachable_resultsIn500InternalError(Vertx vertx, VertxTestContext tc) {
    String fxBaseUrl = "http://localhost:1";
    String promoBaseUrl = "http://localhost:" + promo.port();
    var verticle = new QuoteVerticle(0, fxBaseUrl, promoBaseUrl, 50, 50, 2);

    vertx.deployVerticle(verticle)
      .map(ignored -> verticle.actualPort())
      .compose(port -> sendQuoteRequest(vertx, port, requestBody(10.00, "USD", "ECONOMY", "NONE", null), null))
      .onSuccess(resp -> tc.verify(() -> {
        assertThat(resp.statusCode()).isEqualTo(500);
        var json = resp.bodyAsJsonObject();
        assertThat(json.getString("code")).isEqualTo("INTERNAL_ERROR");
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  private io.vertx.core.Future<Integer> deployApp(Vertx vertx, long fxTimeoutMs, long promoTimeoutMs, int fxMaxAttempts) {
    String fxBaseUrl = "http://localhost:" + fx.port();
    String promoBaseUrl = "http://localhost:" + promo.port();
    var verticle = new QuoteVerticle(0, fxBaseUrl, promoBaseUrl, fxTimeoutMs, promoTimeoutMs, fxMaxAttempts);

    return vertx.deployVerticle(verticle)
      .map(ignored -> {
        int port = verticle.actualPort();
        if (port <= 0) {
          throw new IllegalStateException("Server did not bind to a port");
        }
        return port;
      });
  }

  private static JsonObject requestBody(double fareAmount, String currency, String cabinClass, String customerTier, String promoCode) {
    var json = new JsonObject()
      .put("fareAmount", fareAmount)
      .put("currency", currency)
      .put("cabinClass", cabinClass)
      .put("customerTier", customerTier);

    if (promoCode != null) {
      json.put("promoCode", promoCode);
    }
    return json;
  }

  private static io.vertx.core.Future<HttpResponse<Buffer>> sendQuoteRequest(
    Vertx vertx,
    int port,
    JsonObject body,
    String requestIdHeader
  ) {
    var client = WebClient.create(vertx);

    var req = client.post(port, "localhost", "/v1/points/quote")
      .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json");

    if (requestIdHeader != null) {
      req.putHeader("X-Request-Id", requestIdHeader);
    }

    return req.sendBuffer(Buffer.buffer(body.encode()))
      .map(resp -> {
        assertThat(resp.getHeader("content-type")).contains("application/json");
        assertThat(resp.getHeader("x-request-id")).isNotBlank();
        return resp;
      });
  }

  private static List<String> stringList(JsonObject json, String field) {
    var arr = json.getJsonArray(field);
    if (arr == null) {
      return List.of();
    }
    return arr.stream().map(String::valueOf).toList();
  }
}
