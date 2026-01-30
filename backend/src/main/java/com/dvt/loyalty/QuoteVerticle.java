package com.dvt.loyalty;

import com.dvt.loyalty.api.ErrorResponse;
import com.dvt.loyalty.api.PointsQuoteRequest;
import com.dvt.loyalty.client.FxClient;
import com.dvt.loyalty.client.PromoClient;
import com.dvt.loyalty.client.UpstreamException;
import com.dvt.loyalty.service.QuoteService;
import com.dvt.loyalty.service.ValidationException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.UUID;

/**
 * HTTP layer - sets up the Vert.x server and routes requests to QuoteService.
 * All the business logic lives in QuoteService; this just handles HTTP stuff.
 */
public final class QuoteVerticle extends AbstractVerticle {

  private final int port;
  private final String fxBaseUrl;
  private final String promoBaseUrl;
  private final long fxTimeoutMs;
  private final long promoTimeoutMs;
  private final int fxMaxAttempts;
  private volatile int actualPort = -1;

  // Default constructor reads config from env vars - used when running for real
  public QuoteVerticle() {
    this(Integer.parseInt(System.getenv().getOrDefault("PORT", "8080")));
  }

  // Constructor with just port - still reads other config from env
  public QuoteVerticle(int port) {
    this(
      port,
      System.getenv().getOrDefault("FX_BASE_URL", "http://localhost:8081"),
      System.getenv().getOrDefault("PROMO_BASE_URL", "http://localhost:8082"),
      Long.parseLong(System.getenv().getOrDefault("FX_TIMEOUT_MS", "500")),
      Long.parseLong(System.getenv().getOrDefault("PROMO_TIMEOUT_MS", "300")),
      Integer.parseInt(System.getenv().getOrDefault("FX_MAX_ATTEMPTS", "3"))
    );
  }

  public QuoteVerticle(
    int port,
    String fxBaseUrl,
    String promoBaseUrl,
    long fxTimeoutMs,
    long promoTimeoutMs,
    int fxMaxAttempts
  ) {
    this.port = port;
    this.fxBaseUrl = fxBaseUrl;
    this.promoBaseUrl = promoBaseUrl;
    this.fxTimeoutMs = fxTimeoutMs;
    this.promoTimeoutMs = promoTimeoutMs;
    this.fxMaxAttempts = fxMaxAttempts;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    var router = Router.router(vertx);

    var quoteService = new QuoteService(
      new FxClient(vertx, fxBaseUrl, fxTimeoutMs),
      new PromoClient(vertx, promoBaseUrl, promoTimeoutMs),
      fxMaxAttempts
    );

    router.route().handler(BodyHandler.create());

    router.post("/v1/points/quote").handler(ctx -> {
      // For tracing/debugging - echo back the request ID or generate one
      String requestId = ctx.request().getHeader("X-Request-Id");
      if (requestId == null || requestId.isBlank()) {
        requestId = UUID.randomUUID().toString();
      }

      ctx.response()
        .putHeader("X-Request-Id", requestId)
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json");

      PointsQuoteRequest request;
      try {
        request = Json.decodeValue(ctx.body().buffer(), PointsQuoteRequest.class);
      } catch (Exception e) {
        ctx.response().setStatusCode(400)
          .end(Json.encode(new ErrorResponse("INVALID_JSON", "Request body must be valid JSON", java.util.List.of())));
        return;
      }

      quoteService.quote(request)
        .onSuccess(response -> ctx.response().setStatusCode(200).end(Json.encode(response)))
        .onFailure(err -> {
          if (err instanceof ValidationException ve) {
            ctx.response().setStatusCode(400)
              .end(Json.encode(new ErrorResponse("VALIDATION_ERROR", "Request validation failed", ve.details())));
            return;
          }
          if (err instanceof UpstreamException ue) {
            ctx.response().setStatusCode(502)
              .end(Json.encode(new ErrorResponse("UPSTREAM_ERROR", ue.getMessage(), java.util.List.of(ue.upstream()))));
            return;
          }

          ctx.response().setStatusCode(500)
            .end(Json.encode(ErrorResponse.simple("INTERNAL_ERROR", "Unexpected error")));
        });
    });

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(port)
      .onSuccess(server -> {
        actualPort = server.actualPort();
        System.out.println("Listening on " + actualPort);
        startPromise.complete();
      })
      .onFailure(err -> {
        startPromise.fail(err);
        err.printStackTrace();
      });
  }

  public int actualPort() {
    return actualPort;
  }
}
