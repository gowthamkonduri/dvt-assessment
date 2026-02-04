# Loyalty Points Quote Service (Backend)

Java 21 + Maven + Vert.x HTTP service for quoting loyalty points.

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)

## Overview

This service calculates loyalty points for airline fare purchases. It integrates with:
- **FX Service**: Converts fare amounts to a base currency for points calculation
- **Promo Service**: Applies promotional bonus points when valid promo codes are provided

### Key Features

- Real-time points calculation with tier-based bonuses
- Graceful degradation when upstream services fail
- Configurable retry logic for FX service
- Request tracing via `X-Request-Id` header
- Comprehensive validation with descriptive error messages

## Quick Start

### Prerequisites

- **Java 21+** - Ensure JDK 21 is installed and configured
- **Maven 3.6+** - For building and running tests

### Running the Service

```bash
# Run tests
mvn test

# Run tests with coverage enforcement
mvn verify

# Start the service locally
mvn -q exec:java
```

The service starts on port 8080 by default.

## API Documentation

### POST /v1/points/quote

Calculate loyalty points for a fare purchase.

#### Request

```json
{
  "fareAmount": 1234.50,
  "currency": "USD",
  "cabinClass": "ECONOMY",
  "customerTier": "SILVER",
  "promoCode": "SUMMER25"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `fareAmount` | number | Yes | Fare amount (0.01 - 1,000,000.00) |
| `currency` | string | Yes | Currency code: `USD`, `GBP`, `EUR`, `AUD` |
| `cabinClass` | string | Yes | Cabin class: `ECONOMY`, `BUSINESS`, `FIRST` |
| `customerTier` | string | Yes | Tier: `NONE`, `SILVER`, `GOLD`, `PLATINUM` |
| `promoCode` | string | No | Promotional code (uppercase alphanumeric, max 20 chars) |

#### Response (Success - 200)

```json
{
  "basePoints": 1234,
  "tierBonus": 185,
  "promoBonus": 308,
  "totalPoints": 1727,
  "effectiveFxRate": 3.67,
  "warnings": ["PROMO_EXPIRES_SOON"]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `basePoints` | integer | Points from fare amount (truncated, not rounded) |
| `tierBonus` | integer | Additional points from customer tier |
| `promoBonus` | integer | Bonus from promotional code |
| `totalPoints` | integer | Total points (capped at 50,000) |
| `effectiveFxRate` | number | Exchange rate applied |
| `warnings` | array | Warning codes (see below) |

**Warning Codes:**
- `PROMO_EXPIRES_SOON` - Promo code expires within 7 days
- `PROMO_UNAVAILABLE` - Promo service was unavailable
- `PROMO_TIMEOUT` - Promo service timed out

#### Response (Validation Error - 400)

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Invalid request parameters",
  "details": ["fareAmount must be > 0 (received: -100.0)"]
}
```

#### Response (Invalid JSON - 400)

```json
{
  "code": "INVALID_JSON",
  "message": "Failed to parse request body",
  "details": ["Unexpected character at position 10"]
}
```

#### Response (Upstream Error - 502)

```json
{
  "code": "UPSTREAM_ERROR",
  "message": "External service error",
  "details": ["FX"]
}
```

### Headers

| Header | Direction | Description |
|--------|-----------|-------------|
| `X-Request-Id` | Request/Response | Request correlation ID (auto-generated if not provided) |
| `Content-Type` | Both | Always `application/json` |

### Example cURL

```bash
curl -X POST http://localhost:8080/v1/points/quote \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: my-trace-123" \
  -d '{
    "fareAmount": 500.00,
    "currency": "USD",
    "cabinClass": "ECONOMY",
    "customerTier": "GOLD",
    "promoCode": "SUMMER25"
  }'
```

## Configuration

All configuration is via environment variables with sensible defaults.

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8080` | HTTP server port |
| `FX_BASE_URL` | `http://localhost:8081` | FX service base URL |
| `PROMO_BASE_URL` | `http://localhost:8082` | Promo service base URL |
| `FX_TIMEOUT_MS` | `500` | FX service request timeout (ms) |
| `PROMO_TIMEOUT_MS` | `300` | Promo service request timeout (ms) |
| `FX_MAX_ATTEMPTS` | `3` | Max retry attempts for FX service |

### Example Docker Run

```bash
docker run -p 8080:8080 \
  -e FX_BASE_URL=http://fx-service:8081 \
  -e PROMO_BASE_URL=http://promo-service:8082 \
  -e FX_TIMEOUT_MS=1000 \
  loyalty-quote-service
```

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    QuoteVerticle (HTTP Layer)               │
│  - Request parsing & validation                             │
│  - Response formatting                                      │
│  - Security headers                                         │
│  - Request ID tracking                                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    QuoteService (Business Logic)            │
│  - Points calculation                                       │
│  - Tier bonus computation                                   │
│  - Promo handling with fallback                             │
│  - Points cap enforcement                                   │
└─────────────────────────────────────────────────────────────┘
                    │                   │
                    ▼                   ▼
        ┌───────────────────┐  ┌───────────────────┐
        │     FxClient      │  │    PromoClient    │
        │  - Retry logic    │  │  - Timeout        │
        │  - Rate fetching  │  │  - Graceful fail  │
        └───────────────────┘  └───────────────────┘
                    │                   │
                    ▼                   ▼
            [FX Service]        [Promo Service]
```

### Key Design Decisions (ADRs)

#### ADR-001: Graceful Promo Degradation

**Context:** Promo service may be unavailable or slow.

**Decision:** If promo lookup fails (timeout, error, or invalid response), we proceed with 0 promo bonus and add a warning to the response.

**Rationale:** A failing promo service should not block the entire quote. Users can still see their base points and tier bonus.

#### ADR-002: FX Service Retry Strategy

**Context:** FX service experiences transient failures (5xx errors).

**Decision:** Retry FX calls up to 3 times on 5xx errors. Do not retry on 4xx (client errors).

**Rationale:** 5xx errors are often transient. 4xx indicates a problem with our request that won't resolve with retries.

#### ADR-003: Points Truncation vs Rounding

**Context:** Converting fare amounts to points may result in fractional values.

**Decision:** Truncate (round down) to nearest integer.

**Rationale:** Consistent with industry practice; avoids "free" fractional points.

#### ADR-004: Connection Pooling for HTTP Clients

**Context:** High-volume scenarios require efficient connection management.

**Decision:** Configure WebClient with connection pooling (maxPoolSize=10, keepAlive=true).

**Rationale:** Reduces connection overhead and improves performance under load.

### Business Rules

| Rule | Value | Description |
|------|-------|-------------|
| Max total points | 50,000 | Per-booking cap to prevent abuse |
| Min fare amount | 0.01 | Prevents micro-transaction gaming |
| Max fare amount | 1,000,000.00 | Prevents data entry errors |
| Promo code max length | 20 | Security: prevents injection |
| Promo expiry warning | 7 days | Warns when promo expires soon |

### Tier Bonuses

| Tier | Bonus Rate |
|------|------------|
| NONE | 0% |
| SILVER | 15% |
| GOLD | 25% |
| PLATINUM | 50% |

## Testing

### Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=QuoteVerticleComponentTest

# With coverage report
mvn verify
open target/site/jacoco/index.html
```

### Test Categories

| Category | File | Description |
|----------|------|-------------|
| Component | `QuoteVerticleComponentTest` | End-to-end HTTP tests |
| Advanced | `QuoteServiceAdvancedTest` | Boundary, performance, contract tests |
| Unit | `FxClientTest`, `PromoClientTest` | Client-level tests |
| Utility | `RetryTest`, `TimeTest` | Utility function tests |

### Coverage Requirements

- Minimum line coverage: 80%
- Enforced via Maven JaCoCo plugin

## Troubleshooting

### Common Issues

#### Service won't start: "Address already in use"

Another process is using port 8080.

```bash
# Find the process
lsof -i :8080

# Kill it or use a different port
PORT=9090 mvn exec:java
```

#### All requests fail with 502

FX service is unreachable. Check:
1. FX service is running
2. `FX_BASE_URL` is correct
3. Network connectivity between services

```bash
# Test FX connectivity
curl http://localhost:8081/fx/quote?currency=USD&amount=100
```

#### Promo codes always return 0 bonus

Check promo service:
1. Service is running at `PROMO_BASE_URL`
2. Promo code exists and hasn't expired
3. Logs show `PROMO_UNAVAILABLE` or `PROMO_TIMEOUT` warning

#### Requests timing out

Increase timeout configuration:
```bash
FX_TIMEOUT_MS=2000 PROMO_TIMEOUT_MS=1000 mvn exec:java
```

### Logging

Logs use SLF4J with Logback. Key log patterns:

```
# Successful quote
INFO  [request-id] Quote successful: totalPoints=1727

# Validation failure  
WARN  [request-id] Validation failed: [fareAmount must be > 0]

# Upstream retry
WARN  FX service returned error: status=500
INFO  Retrying FX call, attempt 2/3

# Promo fallback
WARN  Promo lookup failed for code=SUMMER25: timeout
```

Adjust log level in `src/main/resources/logback.xml`.

## Development

### Project Structure

```
src/
├── main/java/com/dvt/loyalty/
│   ├── QuoteVerticle.java      # HTTP layer
│   ├── PointsQuoteResponse.java # Response DTO
│   ├── api/                     # Request DTOs, enums
│   ├── client/                  # FX and Promo clients
│   ├── config/                  # AppConfig, BusinessRules
│   ├── service/                 # QuoteService business logic
│   └── util/                    # Retry, Time utilities
└── test/java/com/dvt/loyalty/
    ├── QuoteVerticleComponentTest.java
    ├── QuoteServiceAdvancedTest.java
    └── ...
```

### Adding New Features

1. Update `BusinessRules.java` for new constants
2. Modify `QuoteService.java` for business logic
3. Update `QuoteVerticle.java` for HTTP changes
4. Add tests in appropriate test class
5. Update this README

---

**Version:** 1.0.0  
**Last Updated:** February 2026
