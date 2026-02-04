package com.dvt.loyalty;

import com.dvt.loyalty.api.ErrorResponse;
import com.dvt.loyalty.api.PointsQuoteRequest;
import com.dvt.loyalty.client.FxClient;
import com.dvt.loyalty.client.PromoClient;
import com.dvt.loyalty.client.UpstreamException;
import com.dvt.loyalty.config.AppConfig;
import com.dvt.loyalty.service.QuoteService;
import com.dvt.loyalty.service.ValidationException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * HTTP layer - sets up the Vert.x server and routes requests to QuoteService.
 * All the business logic lives in QuoteService; this just handles HTTP stuff.
 */
public final class QuoteVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(QuoteVerticle.class);

  private final AppConfig config;
  private volatile int actualPort = -1;
  
  // Clients managed for proper lifecycle cleanup
  private FxClient fxClient;
  private PromoClient promoClient;

  // Default constructor reads config from env vars - used when running for real
  public QuoteVerticle() {
    this(AppConfig.fromEnvironment());
  }

  // Constructor with config - allows full configuration control
  public QuoteVerticle(AppConfig config) {
    this.config = config;
    log.debug("QuoteVerticle initialized with config: {}", config);
  }

  // Constructor with just port - useful for tests
  public QuoteVerticle(int port) {
    this(AppConfig.fromEnvironment().withPort(port));
  }

  // Full constructor for backwards compatibility and testing
  public QuoteVerticle(
    int port,
    String fxBaseUrl,
    String promoBaseUrl,
    long fxTimeoutMs,
    long promoTimeoutMs,
    int fxMaxAttempts
  ) {
    this(new AppConfig(port, fxBaseUrl, promoBaseUrl, fxTimeoutMs, promoTimeoutMs, fxMaxAttempts));
  }

  @Override
  public void start(Promise<Void> startPromise) {
    var router = Router.router(vertx);

    // Create clients and store references for cleanup
    fxClient = new FxClient(vertx, config.fxBaseUrl(), config.fxTimeoutMs());
    promoClient = new PromoClient(vertx, config.promoBaseUrl(), config.promoTimeoutMs());
    
    var quoteService = new QuoteService(
      fxClient,
      promoClient,
      config.fxMaxAttempts()
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
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .putHeader("X-Content-Type-Options", "nosniff")
        .putHeader("X-Frame-Options", "DENY")
        .putHeader("X-XSS-Protection", "1; mode=block");

      PointsQuoteRequest request;
      try {
        request = Json.decodeValue(ctx.body().buffer(), PointsQuoteRequest.class);
      } catch (Exception e) {
        log.warn("[{}] Failed to parse request body: {}", requestId, e.getMessage());
        ctx.response().setStatusCode(400)
          .end(Json.encode(new ErrorResponse("INVALID_JSON", "Request body must be valid JSON", List.of(e.getMessage()))));
        return;
      }

      final String reqId = ctx.response().headers().get("X-Request-Id");
      log.info("[{}] Processing quote request: currency={}, fareAmount={}, tier={}, promoCode={}",
        reqId, request.currency(), request.fareAmount(), request.customerTier(), request.promoCode());

      quoteService.quote(request)
        .onSuccess(response -> {
          log.info("[{}] Quote successful: totalPoints={}", reqId, response.totalPoints());
          ctx.response().setStatusCode(200).end(Json.encode(response));
        })
        .onFailure(err -> {
          if (err instanceof ValidationException ve) {
            log.warn("[{}] Validation failed: {}", reqId, ve.details());
            ctx.response().setStatusCode(400)
              .end(Json.encode(new ErrorResponse("VALIDATION_ERROR", "Request validation failed", ve.details())));
            return;
          }
          if (err instanceof UpstreamException ue) {
            log.error("[{}] Upstream error: service={}, status={}", reqId, ue.upstream(), ue.status());
            ctx.response().setStatusCode(502)
              .end(Json.encode(new ErrorResponse("UPSTREAM_ERROR", ue.getMessage(), List.of(ue.upstream()))));
            return;
          }

          log.error("[{}] Unexpected error processing quote", reqId, err);
          ctx.response().setStatusCode(500)
            .end(Json.encode(ErrorResponse.simple("INTERNAL_ERROR", "Unexpected error")));
        });
    });

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(config.port())
      .onSuccess(server -> {
        actualPort = server.actualPort();
        log.info("HTTP server started on port {}", actualPort);
        startPromise.complete();
      })
      .onFailure(err -> {
        log.error("Failed to start HTTP server on port {}", config.port(), err);
        startPromise.fail(err);
      });
  }

  public int actualPort() {
    return actualPort;
  }

  @Override
  public void stop() {
    log.info("Stopping QuoteVerticle, cleaning up resources");
    if (fxClient != null) {
      fxClient.close();
    }
    if (promoClient != null) {
      promoClient.close();
    }
  }
}
