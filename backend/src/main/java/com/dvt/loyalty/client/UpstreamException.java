package com.dvt.loyalty.client;

/**
 * Exception thrown when an upstream service call fails.
 *
 * Upstream identifiers: "FX" (exchange rate service), "PROMO" (promotional code service)
 * Typically mapped to HTTP 502 Bad Gateway with error code "UPSTREAM_ERROR".
 *
 * @see FxClient
 * @see PromoClient
 */
public final class UpstreamException extends RuntimeException {

  private final String upstream;
  private final int status;

  /** Creates a new UpstreamException. */
  public UpstreamException(String upstream, int status, String message) {
    super(message);
    this.upstream = upstream;
    this.status = status;
  }

  /** Returns the upstream service identifier (e.g., "FX", "PROMO"). */
  public String upstream() {
    return upstream;
  }

  /** Returns the HTTP status code from the upstream service. */
  public int status() {
    return status;
  }
}
