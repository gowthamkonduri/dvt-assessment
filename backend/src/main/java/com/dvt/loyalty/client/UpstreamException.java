package com.dvt.loyalty.client;

public final class UpstreamException extends RuntimeException {

  private final String upstream;
  private final int status;

  public UpstreamException(String upstream, int status, String message) {
    super(message);
    this.upstream = upstream;
    this.status = status;
  }

  public String upstream() {
    return upstream;
  }

  public int status() {
    return status;
  }
}
