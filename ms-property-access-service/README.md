# ms-property-access-service

Demo config-client service. Fetches its `name`/`description` properties from `ms-spring-cloud-config-server` and shows how to force a config reload without restarting.

## What it does

Exposes an endpoint that returns two properties (`name`, `description`) sourced from an external, git-backed property file rather than a local `application.properties` — and refreshes those values from the config server on every call.

## Tech stack

- Java 11, Spring Boot `2.3.4.RELEASE` (note: slightly older than the other services, which use `2.3.6.RELEASE`), Spring Cloud `Hoxton.SR9`
- `spring-boot-starter-web`, `spring-boot-starter-actuator`
- `spring-cloud-starter-config` (Config Client)
- `spring-data-rest-hal-explorer`, `springfox-boot-starter` (Swagger/OpenAPI UI — present but not central to the config-demo flow)
- Main class: `PropertyAccessServiceApplication` (no Eureka annotations — this service does not register with the discovery server)

## How it works

1. `bootstrap.yml` sets `spring.cloud.config.uri: http://localhost:8889` (legacy-style config client bootstrap), and `application.yml` additionally sets `spring.config.import: optional:configserver:http://localhost:8889` (the newer Spring Boot 2.4+-style import, used here for compatibility/demo purposes) plus `spring.profiles.active: dev`.
2. On startup, it fetches `ms-property-access-service-dev.properties` from `ms-spring-cloud-config-server`, populating `name`/`description` into `PropertyAccessBean` via `@Value`.
3. `PropertyFileAccessController.accesPropertyFile()` handles `GET /access/accessPropertyFile`:
   - First calls its own private `refreshActuator()`, which issues a `RestTemplate` POST to its **own** `http://localhost:8100/actuator/refresh` — forcing Spring to re-bind `@Value` properties from the config server before responding, simulating dynamic config reload without a full Spring Cloud Bus setup.
   - Then returns a `PropertyAccessValue` built from the (freshly refreshed) `name`/`description`.
4. `management.endpoints.web.exposure.include: refresh` — only the `refresh` actuator endpoint is exposed, deliberately narrow for security since this triggers a config re-fetch.

## Configuration

- `src/main/resources/bootstrap.yml` — `spring.cloud.config.uri`
- `src/main/resources/application.yml` — `spring.config.import`, `spring.profiles.active: dev`, `server.port: ${port:8100}`
- Actual values served: `environment-variable-repo/ms-property-access-service-dev.properties` (on the Config Server side)

## Running it

Requires `ms-spring-cloud-config-server` to be running first (on `:8889`), since this service imports config from it at startup.

```bash
./mvnw spring-boot:run
```

## Endpoints

```
GET  localhost:8100/access/accessPropertyFile
POST localhost:8100/actuator/refresh
```

## Dependencies

Depends on `ms-spring-cloud-config-server`. This flow is standalone — **not** registered with Eureka and **not** routed through either API gateway. See the "Config Server Demo Flow" section of the root [ARCHITECTURE.md](../ARCHITECTURE.md).
