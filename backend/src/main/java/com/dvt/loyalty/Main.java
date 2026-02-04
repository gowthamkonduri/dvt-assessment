package com.dvt.loyalty;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    log.info("Starting application...");
    var vertx = Vertx.vertx();
    vertx.deployVerticle(new QuoteVerticle())
      .onFailure(err -> {
        log.error("Failed to deploy QuoteVerticle", err);
        vertx.close();
      })
      .onSuccess(id -> log.info("QuoteVerticle deployed successfully: {}", id));
  }
}
