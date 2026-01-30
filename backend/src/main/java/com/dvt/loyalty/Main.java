package com.dvt.loyalty;

import io.vertx.core.Vertx;

public final class Main {
  public static void main(String[] args) {
    var vertx = Vertx.vertx();
    vertx.deployVerticle(new QuoteVerticle())
      .onFailure(err -> {
        err.printStackTrace();
        vertx.close();
      });
  }
}
