# ms-eureka-naming-server

Service registry and discovery server. Every other service in this repo registers itself here so it can be found by logical name instead of a hardcoded host:port.

## What it does

Runs a standalone Netflix Eureka server. Client services (with `spring-cloud-starter-netflix-eureka-client` on the classpath) register on startup and send periodic heartbeats; the registry tracks which instances of each service name are currently alive. Consumers (Feign clients, gateways) query this registry to resolve a service name to one or more live instances.

## Tech stack

- Java 11, Spring Boot `2.3.6.RELEASE`, Spring Cloud `Hoxton.SR9`
- `spring-cloud-starter-netflix-eureka-server`
- `spring-boot-starter-actuator`
- `micrometer-core` + `micrometer-registry-prometheus` (metrics)
- Main class: `EurekaNamingServerApplication`, annotated `@EnableEurekaServer`

## How it works

1. On startup, `@EnableEurekaServer` boots an embedded Eureka registry.
2. `register-with-eureka: false` and `fetch-registry: false` — this instance never registers itself or pulls a peer's registry; it runs in **standalone mode**, not as a client of another node.
3. `enable-self-preservation: false` — the registry evicts instances immediately once their heartbeats stop, rather than assuming a network partition (useful for local dev where you don't want stale, unreachable instances lingering in the dashboard).
4. Other services point `eureka.client.serviceUrl.defaultZone` at `http://localhost:8777/eureka` to register and to look up other services.
5. A peer-replicated cluster (`peer1`/`peer2` Spring profiles) is a standard Eureka pattern this setup could be extended to, but no profile-specific config files exist in this repo currently — it runs standalone only.

## Configuration

`src/main/resources/application.yml`:
- `server.port: ${port:8777}` — override with `--port=<n>` or `-Dport=<n>` if 8777 is taken.
- Actuator endpoint exposure and Sleuth/Zipkin blocks are present but commented out.

## Running it

```bash
./mvnw spring-boot:run
# or
./mvnw clean package -DskipTests
java -jar target/*.jar
```

Dashboard: `http://localhost:8777` — lists every currently registered service instance.

> Note: the `Dockerfile` in this module `EXPOSE`s port `8761` (Eureka's conventional default), but `application.yml` actually configures `8777`. If containerizing, expose/map `8777` instead, or override `server.port` to `8761`.

## Who depends on this

`ms-product-stock-service`, `ms-product-enquiry-service`, `ms-zuul-api-gateway-server`, and `ms-spring-cloud-api-gateway-service` all register here. See the root [ARCHITECTURE.md](../ARCHITECTURE.md) for the full communication diagram.
