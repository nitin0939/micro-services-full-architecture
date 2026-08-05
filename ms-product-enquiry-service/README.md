# ms-product-enquiry-service

Orchestrator for product enquiries. Takes a customer-facing request (product, availability, quantity), pulls current price/discount from `ms-product-stock-service`, and returns the final discounted total.

## What it does

Given a product name, availability flag, and unit count, it fetches the product's price/discount from the stock service and computes `totalPrice = price × unit`, then applies the discount percentage to get the final price.

## Tech stack

- Java 11, Spring Boot `2.3.6.RELEASE`, Spring Cloud `Hoxton.SR9`
- `spring-boot-starter-web`, `spring-boot-starter-actuator`
- `spring-cloud-starter-netflix-eureka-client`
- `spring-cloud-starter-openfeign` (declarative REST client)
- `spring-cloud-starter-netflix-ribbon` (client-side load balancing)
- `spring-cloud-starter-bus-amqp` (present, not wired to a broker)
- `io.github.resilience4j:resilience4j-spring-boot2` + `spring-boot-starter-aop` (circuit breaker on the call to `ms-product-stock-service`)
- `micrometer-core` + `micrometer-registry-prometheus`
- Main class: `ProductEnquiryServiceApplication`, annotated `@EnableEurekaClient` and `@EnableFeignClients("com.microservices.productenquiryservice")`

## How it works

1. `ProductEnquiryController.getEnquiryOfProduct(name, availability, unit)` handles:
   `GET /product-enquiry/name/{name}/availability/{availability}/unit/{unit}`
2. It calls `ProductStockService.checkProductStock(name, availability)`, which is annotated `@CircuitBreaker(name = "productStockService", fallbackMethod = "checkProductStockFallback")` and internally calls `ProductStockClient.checkProductStock(name, availability)` — a Feign interface (`@FeignClient(name="ms-product-stock-service")`) mapped to `ms-product-stock-service`'s `GET /check-product-stock/productName/{productName}/productAvailability/{productAvailability}`.
3. Because the Feign client references the stock service by its **Eureka logical name** (not a hardcoded URL — see the commented-out `url="localhost:8800"` variant left in `ProductStockClient.java` for contrast), Ribbon resolves the live instance list from Eureka and load-balances the call across however many `ms-product-stock-service` instances are registered.
4. With the returned price/discount, it computes:
   `totalPrice = productPrice * unit`
   `discountPrice = totalPrice - totalPrice * discountOffer / 100`
5. Returns a `ProductEnquiryBean` including the `port` of the stock-service instance that answered, so repeated calls visibly rotate across instances — see [Load Balancing Across Multiple Instances](../README.md#load-balancing-across-multiple-instances) in the root README.
6. If `ms-product-stock-service` is unreachable/failing enough to trip the breaker, `checkProductStockFallback` runs instead and throws `ProductStockServiceUnavailableException`, which `ProductEnquiryController`'s `@ExceptionHandler` converts into an HTTP `503` response — see [Circuit Breaker](#circuit-breaker) below.

## Circuit Breaker

The call to `ms-product-stock-service` is wrapped in a Resilience4j circuit breaker (instance name `productStockService`, configured in `application.yml` under `resilience4j.circuitbreaker.instances.productStockService`):

- **Closed** (normal) — calls pass straight through.
- **Open** — trips once ≥50% of the last 10 calls fail (`failureRateThreshold: 50`, `slidingWindowSize: 10`, `minimumNumberOfCalls: 5`); for the next 10 seconds (`waitDurationInOpenState: 10s`) every call short-circuits straight to the fallback instead of hitting the network.
- **Half-Open** — after the wait, 3 trial calls (`permittedNumberOfCallsInHalfOpenState: 3`) decide whether to close again or reopen.
- Circuit state is exposed on `/actuator/health` (`registerHealthIndicator: true`).

Relevant classes:
- `service/ProductStockService.java` — the `@CircuitBreaker`-annotated method and its fallback.
- `exception/ProductStockServiceUnavailableException.java` — thrown by the fallback.
- `controller/ProductEnquiryController.java` — the `@ExceptionHandler` that turns that exception into a `503`.

Full write-up with a sequence diagram: [ARCHITECTURE.md § Circuit Breaker (Resilience4j)](../ARCHITECTURE.md#circuit-breaker-resilience4j).

## Configuration

`src/main/resources/application.yml`:
- `server.port: ${port:8700}`
- `eureka.client.serviceUrl.defaultZone: http://localhost:8777/eureka`
- Sleuth disabled; Zipkin block commented out.
- A commented-out static Ribbon list (`ms-product-stock-service.ribbon.listOfServers`) is left in the file as an alternative to Eureka-based resolution — currently disabled in favor of discovery.

## Running it

```bash
./mvnw spring-boot:run
```

## Endpoint

```
GET /product-enquiry/name/{name}/availability/{availability}/unit/{unit}
```

Example: `GET localhost:8700/product-enquiry/name/bat/availability/yes/unit/3`

## Docker

```bash
docker build -t ms-product-enquiry-service .
docker run -p 8700:8700 ms-product-enquiry-service
```

## Dependencies

- Calls `ms-product-stock-service` (Feign + Ribbon, Eureka-resolved).
- Registers with `ms-eureka-naming-server`.
- Can sit behind `ms-zuul-api-gateway-server` or `ms-spring-cloud-api-gateway-service`. See the root [ARCHITECTURE.md](../ARCHITECTURE.md).
