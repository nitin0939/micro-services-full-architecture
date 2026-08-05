# ms-zuul-api-gateway-server

Legacy-style API Gateway using Netflix Zuul. Sits in front of the product services and routes incoming requests based on path.

## What it does

Provides a single public entry point that forwards requests to the appropriate downstream service, so clients don't need to know each service's individual host/port.

## Tech stack

- Java 11, Spring Boot `2.3.6.RELEASE`, Spring Cloud `Hoxton.SR9`
- `spring-boot-starter-web`
- `spring-cloud-starter-netflix-zuul`
- `spring-cloud-starter-netflix-eureka-client`
- Main class: `ZuulApiGatewayServerApplication`, annotated `@EnableZuulProxy` and `@EnableEurekaClient`

## How it works

1. `@EnableZuulProxy` turns this app into a reverse proxy; routes are declared in `bootstrap.yaml` under `zuul.routes`.
2. Two static routes are configured:
   - `product-enquiry`: `/product-enquiry/**` → `http://localhost:8700/` (single instance)
   - `product-stock`: `/product-stock/**` → `http://localhost:8800/,http://localhost:8801/,http://localhost:8802/` (a comma-separated URL list — Zuul's underlying Ribbon round-robins across these directly, **without** querying Eureka for the current instance list)
3. It also registers with Eureka, which additionally enables **service-name-based routing** for any registered service, e.g. `/ms-product-enquiry-service/**` is automatically proxied to the `ms-product-enquiry-service` instances Eureka knows about — no explicit route needed.
4. Because the `product-stock` route uses a fixed URL list rather than Eureka discovery, adding a 4th stock instance would require updating `bootstrap.yaml`, unlike the Feign-based load balancing in `ms-product-enquiry-service`. See [Load Balancing Across Multiple Instances](../README.md#load-balancing-across-multiple-instances) in the root README for the comparison.

## Configuration

`src/main/resources/bootstrap.yaml`:
- `server.port: ${port:8766}`
- `eureka.client.serviceUrl.defaultZone: http://localhost:8777/eureka`
- `zuul.routes.*` — static path-to-URL mappings described above.

## Running it

```bash
./mvnw spring-boot:run
```

## Endpoints

Static routes:
```
GET localhost:8766/product-enquiry/name/bat/availability/yes/unit/3
GET localhost:8766/product-stock/... (not explicitly mapped in this repo's controllers — use service-name routing below instead)
```

Eureka service-name routing:
```
GET localhost:8766/ms-product-enquiry-service/product-enquiry/name/bat/availability/yes/unit/3
GET localhost:8766/ms-product-stock-service/check-product-stock/productName/bat/productAvailability/yes
```

## Dependencies

Routes to `ms-product-enquiry-service` and `ms-product-stock-service`, and registers with `ms-eureka-naming-server`. Runs as an alternative to `ms-spring-cloud-api-gateway-service` — the two gateways are not chained together. See the root [ARCHITECTURE.md](../ARCHITECTURE.md).
