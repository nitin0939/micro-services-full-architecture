# ms-spring-cloud-config-server

Centralized configuration server. Serves property files to client services instead of each service bundling its own config internally.

## What it does

Exposes an HTTP API that config-client services query to fetch environment-specific properties (e.g. `name`, `description`) at startup or on refresh, backed by a Git repository rather than a local filesystem.

## Tech stack

- Java 11, Spring Boot `2.3.6.RELEASE`, Spring Cloud `Hoxton.SR9`
- `spring-boot-starter-web`
- `spring-cloud-config-server`
- Main class: `SpringCloudConfigServerApplication`, annotated `@EnableConfigServer`

## How it works

1. `@EnableConfigServer` turns this app into a Spring Cloud Config Server.
2. Its git backend, configured in `application.yml` under `spring.cloud.config.server.git`, points at **this same repository** (`https://github.com/nitin0939/micro-services-full-architecture`), restricted to the `environment-variable-repo` folder (`search-paths`) on the `master` branch (`default-label`) — i.e. the repo serves its own `environment-variable-repo/` directory as config data.
3. Property files follow the `{application-name}-{profile}.properties` convention, e.g. `ms-property-access-service-dev.properties`, `ms-property-access-service-qa.properties` — the Config Server resolves the right file based on the requesting client's `spring.application.name` and active profile.
4. Client services (currently `ms-property-access-service`) fetch config via `spring.cloud.config.uri` / `spring.config.import` pointed at this server's URL.

## Configuration

`src/main/resources/application.yml`:
- `server.port: ${port:8889}`
- `spring.cloud.config.server.git.uri/search-paths/default-label` as described above.

## Running it

```bash
./mvnw spring-boot:run
```

## Verifying config resolution directly

Spring Cloud Config exposes a standard lookup endpoint:
```
GET localhost:8889/{application-name}/{profile}
# e.g.
GET localhost:8889/ms-property-access-service/dev
```

## Dependencies

Serves configuration to `ms-property-access-service`. This flow is independent of Eureka and the API gateways — see the "Config Server Demo Flow" section of the root [ARCHITECTURE.md](../ARCHITECTURE.md).
