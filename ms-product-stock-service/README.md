# ms-product-stock-service

Owns product stock data — price, availability, and discount offer — backed by an in-memory H2 database. Designed to be run as multiple instances behind Eureka/Ribbon to demonstrate load balancing.

## What it does

Exposes a single read endpoint that looks up a product by name and availability and returns its price/discount/availability, plus the port of the instance that answered (so callers can see which instance served the request).

## Tech stack

- Java 11, Spring Boot `2.3.6.RELEASE`, Spring Cloud `Hoxton.SR9`
- `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `h2` (runtime, in-memory DB)
- `spring-cloud-starter-netflix-eureka-client`
- `spring-cloud-starter-sleuth` / `spring-cloud-sleuth-zipkin` (present, disabled)
- `spring-cloud-starter-bus-amqp` (present, not wired to a broker)
- `micrometer-core` + `micrometer-registry-prometheus`
- Main class: `ProductStockServiceApplication`, annotated `@EnableEurekaClient`

## How it works

1. `data.sql` seeds the H2 `product_stock` table on startup with 3 rows: bat (₹5000, 20% off), ball (₹500, 40% off), helmet (₹3000, 30% off) — all `productAvailability = "yes"`.
2. `ProductStockController.checkProductStock(productName, productAvailability)` handles:
   `GET /check-product-stock/productName/{productName}/productAvailability/{productAvailability}`
3. It delegates to `ProductStcokRepository.findByProductNameAndProductAvailability(...)` (Spring Data JPA) to fetch the matching `ProductStock` entity.
4. The entity is mapped into a `ProductStockBean` (`id`, `productName`, `productPrice`, `productAvailability`, `discountOffer`) with an added `port` field, read from `Environment.getProperty("local.server.port")` — this is what lets you see load balancing happen across instances.
5. On startup the service registers with `ms-eureka-naming-server` under the app name `ms-product-stock-service`, so any number of instances share one logical service identity in the registry.

## Configuration

`src/main/resources/application.yml`:
- `server.port: ${port:8800}` — override to run additional instances (e.g. `--port=8801`, `--port=8802`).
- `eureka.client.serviceUrl.defaultZone: http://localhost:8777/eureka`
- Sleuth enabled: `false`; Zipkin block present but commented out.
- All actuator endpoints exposed (`management.endpoints.web.exposure.include: "*"`).

## Running multiple instances (for load balancing)

```bash
./mvnw clean package -DskipTests
java -jar target/*.jar --port=8800 &
java -jar target/*.jar --port=8801 &
java -jar target/*.jar --port=8802 &
```

All three register as `ms-product-stock-service` in Eureka. `ms-product-enquiry-service`'s Feign client and `ms-zuul-api-gateway-server`'s route both distribute requests across them — see [Load Balancing Across Multiple Instances](../README.md#load-balancing-across-multiple-instances) in the root README.

## Endpoint

```
GET /check-product-stock/productName/{productName}/productAvailability/{productAvailability}
```

Example: `GET localhost:8800/check-product-stock/productName/bat/productAvailability/yes`

## Docker

```bash
docker build -t ms-product-stock-service .
docker run -p 8800:8800 ms-product-stock-service
```

## Who calls this

`ms-product-enquiry-service` (via Feign/Ribbon) and `ms-zuul-api-gateway-server` (via a static Ribbon route list). See the root [ARCHITECTURE.md](../ARCHITECTURE.md).

## Circuit breaker on the caller side

This service itself has no circuit breaker — it just answers requests. But `ms-product-enquiry-service`'s call into this service is wrapped in a Resilience4j circuit breaker (`productStockService` instance, `@CircuitBreaker` on `ProductStockService.checkProductStock`). If every instance of this service is down, slow, or erroring enough to trip the breaker (≥50% failures over the last 10 calls), the enquiry service stops sending requests here for 10 seconds and fails fast with an HTTP `503` instead of piling up hanging calls against a struggling instance. See [ms-product-enquiry-service's README](../ms-product-enquiry-service/README.md#circuit-breaker) and [ARCHITECTURE.md § Circuit Breaker (Resilience4j)](../ARCHITECTURE.md#circuit-breaker-resilience4j) for the full mechanics.
