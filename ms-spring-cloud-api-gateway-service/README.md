# ms-spring-cloud-api-gateway-service

Modern, reactive alternative to the Zuul gateway, built on Spring Cloud Gateway.

## What it does

Provides the same kind of routing role as `ms-zuul-api-gateway-server` — a single entry point that forwards requests to a downstream service — but using Spring's current reactive gateway stack instead of the legacy Netflix Zuul.

## Tech stack

- Java 11, Spring Boot `2.3.6.RELEASE`, Spring Cloud `Hoxton.SR9`
- `spring-cloud-starter-gateway` (reactive, built on Spring WebFlux/Netty)
- `spring-boot-starter-actuator`
- `spring-cloud-starter-netflix-eureka-client`
- `spring-cloud-starter-sleuth` / `spring-cloud-sleuth-zipkin` (present, disabled)
- `spring-cloud-starter-bus-amqp` (present, not wired to a broker)
- `micrometer-core` + `micrometer-registry-prometheus`
- Main class: `SpringCloudApiGatewayServiceApplication`

## How it works

1. Routes are declared directly in `application.yml` under `spring.cloud.gateway.routes` (rather than a separate Java route builder).
2. One route is configured: `id: stock-enquiry`, predicate `Path=/product-enquiry/**`, forwarding to `uri: http://localhost:8700/` — a hardcoded URI, not `lb://ms-product-enquiry-service`, so this route does not currently load-balance via Eureka even though the app is registered with it.
3. It registers with `ms-eureka-naming-server` on startup, which would allow `lb://<service-name>` style routes if added later.
4. There is currently no route to `ms-product-stock-service`, so this gateway doesn't demonstrate the multi-instance load-balancing scenario the way `ms-zuul-api-gateway-server` does.

## Configuration

`src/main/resources/application.yml`:
- `server.port: ${port:8900}`
- `eureka.client.serviceUrl.defaultZone: http://localhost:8777/eureka`
- `spring.cloud.gateway.routes` — the `stock-enquiry` route described above.
- Zipkin disabled; full actuator endpoint exposure with health details.

## Running it

```bash
./mvnw spring-boot:run
```

## Endpoint

```
GET localhost:8900/product-enquiry/name/bat/availability/yes/unit/3
```
(proxies to `ms-product-enquiry-service` on `:8700`)

## Docker

```bash
docker build -t ms-spring-cloud-api-gateway-service .
docker run -p 8900:8900 ms-spring-cloud-api-gateway-service
```

## Dependencies

Routes to `ms-product-enquiry-service` and registers with `ms-eureka-naming-server`. Runs as an alternative to `ms-zuul-api-gateway-server` — the two gateways are not chained together. See the root [ARCHITECTURE.md](../ARCHITECTURE.md).
