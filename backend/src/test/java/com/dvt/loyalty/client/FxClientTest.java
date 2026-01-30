package com.dvt.loyalty.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class FxClientTest {

  private WireMockServer fx;

  @BeforeEach
  void startWireMock() {
    fx = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    fx.start();
  }

  @AfterEach
  void stopWireMock() {
    if (fx != null) {
      fx.stop();
    }
  }

  @Test
  void quote_success_parsesResponse(Vertx vertx, VertxTestContext tc) {
    fx.stubFor(get(urlPathEqualTo("/fx/quote"))
      .withQueryParam("currency", equalTo("USD"))
      .withQueryParam("amount", equalTo("10.00"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{" +
          "\"convertedAmount\":12.34," +
          "\"rate\":1.234" +
          "}")));

    var client = new FxClient(vertx, "http://localhost:" + fx.port(), 500);

    client.quote("USD", new BigDecimal("10.00"))
      .onSuccess(resp -> tc.verify(() -> {
        assertThat(resp.convertedAmount()).isEqualByComparingTo("12.34");
        assertThat(resp.rate()).isEqualTo(1.234);
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  @Test
  void quote_non200_failsWithUpstreamException(Vertx vertx, VertxTestContext tc) {
    fx.stubFor(get(urlPathEqualTo("/fx/quote"))
      .willReturn(aResponse().withStatus(503)));

    var client = new FxClient(vertx, "http://localhost:" + fx.port(), 500);

    client.quote("USD", new BigDecimal("10.00"))
      .onComplete(ar -> tc.verify(() -> {
        assertThat(ar.failed()).isTrue();
        assertThat(ar.cause()).isInstanceOf(UpstreamException.class);
        var ue = (UpstreamException) ar.cause();
        assertThat(ue.upstream()).isEqualTo("FX");
        assertThat(ue.status()).isEqualTo(503);
        tc.completeNow();
      }));
  }

  @Test
  void quote_invalidJson_failsWithUpstreamException(Vertx vertx, VertxTestContext tc) {
    fx.stubFor(get(urlPathEqualTo("/fx/quote"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("not-json")));

    var client = new FxClient(vertx, "http://localhost:" + fx.port(), 500);

    client.quote("USD", new BigDecimal("10.00"))
      .onComplete(ar -> tc.verify(() -> {
        assertThat(ar.failed()).isTrue();
        assertThat(ar.cause()).isInstanceOf(UpstreamException.class);
        var ue = (UpstreamException) ar.cause();
        assertThat(ue.upstream()).isEqualTo("FX");
        assertThat(ue.status()).isEqualTo(502);
        tc.completeNow();
      }));
  }

  @Test
  void quote_missingFields_failsWithUpstreamException(Vertx vertx, VertxTestContext tc) {
    fx.stubFor(get(urlPathEqualTo("/fx/quote"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{" +
          "\"rate\":1.0" +
          "}")));

    var client = new FxClient(vertx, "http://localhost:" + fx.port(), 500);

    client.quote("USD", new BigDecimal("10.00"))
      .onComplete(ar -> tc.verify(() -> {
        assertThat(ar.failed()).isTrue();
        assertThat(ar.cause()).isInstanceOf(UpstreamException.class);
        var ue = (UpstreamException) ar.cause();
        assertThat(ue.upstream()).isEqualTo("FX");
        assertThat(ue.status()).isEqualTo(502);
        tc.completeNow();
      }));
  }
}
