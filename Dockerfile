# Builds the CRUD API (alert management). The scheduled price checks/emails are handled by
# the GitHub Actions workflow (.github/workflows/check-prices.yml), not by this container —
# ENABLE_INTERNAL_SCHEDULER defaults to false to avoid sending duplicate emails.

FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle installDist --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/install/flight-price-alert /app
EXPOSE 8080
ENTRYPOINT ["/app/bin/flight-price-alert"]
