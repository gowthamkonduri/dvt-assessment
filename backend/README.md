# Loyalty Points Quote Service (Backend)

Java 21 + Maven + Vert.x HTTP service for quoting loyalty points.

## Run

- Tests: `mvn test`
- Coverage + enforce thresholds: `mvn verify`
- Run locally: `mvn -q exec:java`

### Java version

This project targets **Java 21**. Ensure you have JDK 21 installed and configured as your default.

## Endpoint

- `POST /v1/points/quote`

## Notes / Assumptions (to be finalized)

- External services are stubbed in tests using WireMock:
  - FX service: provides an `effectiveFxRate` used during quote calculation.
  - Promo service: provides a promo bonus and (optionally) an expiry date used for warnings.
- Example values in the PDF are treated as illustrative; tests assert our defined rules.
