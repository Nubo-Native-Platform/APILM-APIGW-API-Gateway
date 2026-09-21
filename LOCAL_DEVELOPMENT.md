# Local development: api-gateway

This project is the non-SSO reactive API gateway (data plane). It pulls its
route catalog and user data from the control-plane `api-gateway-service` and
caches values in Redis.

## Prerequisites

- JDK 21 and Maven 3.9 or later.
- Access to the organisation Maven repositories. The POM requires the
  `com.nubons:abstract-nnp:1.0.0` parent and the API-ecosystem shared library.
  Configure the approved Maven `settings.xml`, or install the matching internal
  artifacts before building.
- A running **Redis** (default `localhost:6379`).
- A running **api-gateway-service** (control plane) with at least one route.

## Configure the runtime

All configuration lives in a single `src/main/resources/application.yaml`.
Override values with environment variables instead of editing the file:

```bash
export NNP_APIECOSYSTEM_SERVICE_URL='http://localhost:8082'   # control plane
export GATEWAY_BASE_URL='http://localhost:8080'               # this gateway
export NNP_APIECOSYSTEM_CONSUMER_ANONYMOUS_ID='<consumer-id>'
export SPRING_DATA_REDIS_HOST='localhost'
```

See the [Configuration Reference](README.md#configuration-reference) for the
full key list.

## Build and run

```bash
mvn clean verify
mvn spring-boot:run
```

The gateway listens on `server.port` (default `8080`). On startup it primes
the route-refresh baseline against the control plane; the log confirms the
first successful poll.

## Useful checks

```bash
mvn test
```

If startup fails on an unresolved `${...}` placeholder, set that key via its
environment variable rather than adding environment-specific values to source
control. A Redis connection error means the configured Redis instance is not
reachable.
