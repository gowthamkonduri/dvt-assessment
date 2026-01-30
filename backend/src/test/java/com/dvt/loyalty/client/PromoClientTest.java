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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class PromoClientTest {

  private WireMockServer promo;

  @BeforeEach
  void startWireMock() {
    promo = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    promo.start();
  }

  @AfterEach
  void stopWireMock() {
    if (promo != null) {
      promo.stop();
    }
  }

  @Test
  void fetch_success_parsesResponse(Vertx vertx, VertxTestContext tc) {
    promo.stubFor(get(urlEqualTo("/promo/SUMMER25"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{" +
          "\"bonusPoints\":123," +
          "\"expiresAt\":\"2026-01-01T00:00:00Z\"" +
          "}")));

    var client = new PromoClient(vertx, "http://localhost:" + promo.port(), 500);

    client.fetch("SUMMER25")
      .onSuccess(resp -> tc.verify(() -> {
        assertThat(resp.bonusPoints()).isEqualTo(123);
        assertThat(resp.expiresAt()).isEqualTo("2026-01-01T00:00:00Z");
        tc.completeNow();
      }))
      .onFailure(tc::failNow);
  }

  @Test
  void fetch_non200_failsWithUpstreamException(Vertx vertx, VertxTestContext tc) {
    promo.stubFor(get(urlEqualTo("/promo/SUMMER25"))
      .willReturn(aResponse().withStatus(404)));

    var client = new PromoClient(vertx, "http://localhost:" + promo.port(), 500);

    client.fetch("SUMMER25")
      .onComplete(ar -> tc.verify(() -> {
        assertThat(ar.failed()).isTrue();
        assertThat(ar.cause()).isInstanceOf(UpstreamException.class);
        var ue = (UpstreamException) ar.cause();
        assertThat(ue.upstream()).isEqualTo("PROMO");
        assertThat(ue.status()).isEqualTo(404);
        tc.completeNow();
      }));
  }

  @Test
  void fetch_nullBody_failsWithUpstreamException(Vertx vertx, VertxTestContext tc) {
    promo.stubFor(get(urlEqualTo("/promo/SUMMER25"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("null")));

    var client = new PromoClient(vertx, "http://localhost:" + promo.port(), 500);

    client.fetch("SUMMER25")
      .onComplete(ar -> tc.verify(() -> {
        assertThat(ar.failed()).isTrue();
        assertThat(ar.cause()).isInstanceOf(UpstreamException.class);
        var ue = (UpstreamException) ar.cause();
        assertThat(ue.upstream()).isEqualTo("PROMO");
        assertThat(ue.status()).isEqualTo(502);
        tc.completeNow();
      }));
  }

  @Test
  void fetch_invalidJson_failsWithUpstreamException(Vertx vertx, VertxTestContext tc) {
    promo.stubFor(get(urlEqualTo("/promo/SUMMER25"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("not-json")));

    var client = new PromoClient(vertx, "http://localhost:" + promo.port(), 500);

    client.fetch("SUMMER25")
      .onComplete(ar -> tc.verify(() -> {
        assertThat(ar.failed()).isTrue();
        assertThat(ar.cause()).isInstanceOf(UpstreamException.class);
        var ue = (UpstreamException) ar.cause();
        assertThat(ue.upstream()).isEqualTo("PROMO");
        assertThat(ue.status()).isEqualTo(502);
        tc.completeNow();
      }));
  }
}
