# micro-services-full-architecture
youtube video link -> https://www.youtube.com/watch?v=Z7A_M8HkJG0

## Architecture
![Architecture](ms-property-access-service/architecture.png)

## Services Overview

### ms-eureka-naming-server (Port: 8777)
Service registry and discovery server built with Netflix Eureka. All microservices register themselves here, enabling them to discover and communicate with each other without hardcoded URLs.
- Built with `spring-cloud-starter-netflix-eureka-server` on Spring Boot 2.3.6 and Spring Cloud Hoxton.SR9
- Runs in standalone mode by default with self-registration and peer replication disabled
- Supports peer replication cluster mode via `peer1` and `peer2` Spring profiles
- Exposes Actuator endpoints for health monitoring and metrics via Micrometer and Prometheus
- Dashboard available at `http://localhost:8777` to view all registered services

### ms-product-stock-service (Port: 8800)
Manages product stock data backed by an H2 in-memory database. Exposes an endpoint to check stock details like product price, availability and discount offers. Supports running on multiple ports for load balancing.
- Uses Spring Data JPA with H2 in-memory database pre-loaded with sample data via `data.sql`
- Sample products loaded: bat (₹5000, 20% off), ball (₹500, 40% off), helmet (₹3000, 30% off)
- Entity fields: `id`, `productName`, `productPrice`, `productAvailability`, `discountOffer`
- Returns the active server port in the response to demonstrate load balancing across multiple instances
- Registered with Eureka for service discovery
- Exposes Actuator and Prometheus metrics endpoints

### ms-product-enquiry-service (Port: 8700)
Handles product enquiry requests from clients. Communicates with `ms-product-stock-service` via Feign Client to fetch stock details, then calculates the total price and discount for the requested quantity.
- Uses `spring-cloud-starter-openfeign` to communicate with `ms-product-stock-service`
- Accepts `productName`, `availability` and `unit` as path variables
- Calculates total price as `productPrice × unit` and applies discount percentage to return final discounted price
- Response includes: `id`, `productName`, `productPrice`, `productAvailability`, `discountOffer`, `unit`, `totalPrice`, `port`
- Registered with Eureka for service discovery
- Supports running on multiple ports for load balancing
- Call to `ms-product-stock-service` is wrapped in a Resilience4j circuit breaker with a fallback that returns HTTP `503` instead of propagating a raw error when the stock service is down (see [ARCHITECTURE.md](ARCHITECTURE.md#circuit-breaker-resilience4j))

### ms-zuul-api-gateway-server (Port: 8766)
API Gateway built with Netflix Zuul. Routes incoming requests to the appropriate downstream microservice and integrates with Eureka for service discovery.
- Built with `spring-cloud-starter-netflix-zuul` and `spring-cloud-starter-netflix-eureka-client`
- Routes `product-enquiry/**` to `ms-product-enquiry-service` on port `8700`
- Routes `product-stock/**` to `ms-product-stock-service` on port `8800`
- Supports service-name based routing via Eureka e.g. `/ms-product-enquiry-service/**`
- Configuration managed via `bootstrap.yaml`

### ms-spring-cloud-api-gateway-service (Port: 8900)
Alternative API Gateway built with Spring Cloud Gateway. Routes requests to downstream services using route configuration.
- Built with `spring-cloud-starter-gateway` as a modern reactive alternative to Zuul
- Routes `/product-enquiry/**` to `ms-product-enquiry-service` on port `8700`
- Registered with Eureka at `http://localhost:8777/eureka`
- Exposes full Actuator endpoints including health with details

### ms-spring-cloud-config-server (Port: 8889)
Centralized configuration server built with Spring Cloud Config. Fetches configuration properties from the `environment-variable-repo` folder in this GitHub repository and serves them to client microservices.
- Built with `spring-cloud-config-server` backed by this GitHub repository
- Reads property files from the `environment-variable-repo` folder on the `master` branch
- Supports multiple environments via profile-based property files e.g. `ms-property-access-service-dev.properties`, `ms-property-access-service-qa.properties`
- Client services connect to it via `http://localhost:8889`

### ms-property-access-service (Port: 8100)
Demonstrates Spring Cloud Config Client integration. Fetches properties from the config server and exposes them via a REST endpoint. Also triggers actuator refresh to reload properties without restarting the service.
- Connects to `ms-spring-cloud-config-server` at `http://localhost:8889` via `bootstrap.yml`
- Active profile set to `dev` by default, fetching `ms-property-access-service-dev.properties` from config server
- Auto-triggers `/actuator/refresh` on every request to pick up latest config changes dynamically
- Exposes `name` and `description` properties fetched from the config server
- Only the `refresh` actuator endpoint is exposed for security
## Load Balancing Across Multiple Instances

`ms-product-stock-service` sets its port as `server.port: ${port:8800}`, so the same jar can be started multiple times with a different port override — e.g. `java -jar ms-product-stock-service.jar --port=8801` and `--port=8802` — to run 3 instances (`8800`, `8801`, `8802`) side by side. Each instance registers with `ms-eureka-naming-server` under the same `spring.application.name` (`ms-product-stock-service`), so Eureka tracks one logical service backed by 3 live instances rather than 3 separate services.

From there, two independent clients load-balance across those instances in different ways:

- **`ms-product-enquiry-service` (Feign + Ribbon, discovery-based):** its `ProductStockClient` is a `@FeignClient(name="ms-product-stock-service")` with no hardcoded URL, so it resolves the live instance list from Eureka at call time and Ribbon picks one using its default round-robin strategy. Because each response carries the answering `port`, calling `/product-enquiry/...` repeatedly shows the port field rotating across `8800`/`8801`/`8802`, confirming requests are actually being spread across instances.
- **`ms-zuul-api-gateway-server` (Ribbon, static list):** its `product-stock` route in `bootstrap.yaml` isn't Eureka-resolved — it lists the instance URLs directly (`http://localhost:8800/,http://localhost:8801/,http://localhost:8802/`), and Zuul's underlying Ribbon round-robins across that fixed list. This works even without Eureka registration, but unlike the Feign approach it won't automatically pick up new instances added later without updating the route config.

`ms-spring-cloud-api-gateway-service` currently only routes `/product-enquiry/**` (not `/product-stock/**` directly), so it doesn't demonstrate load balancing against the stock service in this repo.

## End Points

### Property Access Service
```
GET localhost:8100/access/accessPropertyFile
GET localhost:8100/actuator/refresh
```

### Product Stock Service
> Run stock-service and product-enquiry-service on multiple ports
```
GET localhost:8800/check-product-stock/productName/bat/productAvailability/yes
```

### Product Enquiry Service
```
GET localhost:8700/product-enquiry/name/bat/availability/yes/unit/3
```

### Zuul API Gateway
```
GET localhost:8766/ms-product-enquiry-service/product-enquiry/name/bat/availability/yes/unit/3
GET localhost:8766/ms-product-stock-service/check-product-stock/productName/bat/productAvailability/yes
```

### Spring Cloud API Gateway
```
GET localhost:8900/product-enquiry/name/bat/availability/yes/unit/3
```
