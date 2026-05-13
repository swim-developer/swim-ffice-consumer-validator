# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SWIM FF-ICE Consumer Validator — a mock AISP/EAD service that simulates an external FF-ICE (Flight and Flow Information for a Collaborative Environment) provider for consumer development and testing. Part of the SWIM (System Wide Information Management) reference architecture for Red Hat OpenShift.

This project is a **Consumer Validator**, not a consumer itself. It exposes a Subscription Manager REST API and publishes FF-ICE flight events over AMQP so that a real consumer application can connect to it as if it were a real EUROCONTROL/AISP provider.

## Build & Test

```bash
# Build (always use -DskipTests, never -Dmaven.test.skip=true)
mvn clean package -DskipTests

# Run tests
mvn test

# Run integration tests (must include -DskipITs=false, default skips them)
mvn verify -DskipITs=false

# JaCoCo coverage
mvn test jacoco:report
# Report at: target/site/jacoco/index.html
```

Parent POM (`swim-validators:1.0.0-SNAPSHOT`) must be installed locally first. Key dependency: `swim-validator-consumer` from the same group.

## Architecture

Quarkus application using Hexagonal Architecture (ports & adapters). Depends on shared libraries:
- `swim-validator-consumer` — framework providing `ManageSubscriptionPort` and subscription lifecycle
- `swim-validator-core` — domain model (`QualityOfService`, `SubscriptionResponse`, `SubscriptionStatus`, `CreateSubscriptionCommand`)

### This Project's Code

All code lives in `src/main/java/com/github/swim_developer/validator/ffice/infrastructure/rest/`:
- `SubscriptionManagerResource` — JAX-RS REST controller implementing SWIM SM API (`/swim/v1/subscriptions`, `/swim/v1/topics`, `/swim/v1/features`)
- `ValidatorTopicConfig` — CDI bean providing FF-ICE topic metadata from config properties

### Infrastructure

- **Messaging**: AMQP (ActiveMQ Artemis) for event publishing to consumers
- **Database**: MariaDB for subscription state persistence (Hibernate ORM, auto DDL)
- **Event Generator**: Publishes sample FF-ICE XML events from `src/main/resources/events/` on a cron schedule
- **Heartbeat Publisher**: Periodic heartbeat signals (configurable interval)
- **Container**: UBI9 OpenJDK 21, Containerfile at `src/main/docker/Containerfile.jvm`

### REST API Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/swim/v1/subscriptions` | Create subscription |
| GET | `/swim/v1/subscriptions` | List subscriptions (filter by queue/status) |
| GET | `/swim/v1/subscriptions/{id}` | Get subscription details |
| PUT | `/swim/v1/subscriptions/{id}` | Update subscription status |
| DELETE | `/swim/v1/subscriptions/{id}` | Delete subscription |
| PUT | `/swim/v1/subscriptions/{id}/renew` | Renew subscription |
| GET | `/swim/v1/topics` | List available topics |
| GET | `/swim/v1/topics/{id}` | Get topic details |
| GET | `/swim/v1/features` | WFS GetFeature (stub) |

OpenAPI: `/openapi` — Swagger UI: `/swagger-ui`

### Sample Events

Seven FF-ICE event XML files in `src/main/resources/events/` (FIXM 4.3 format): filed-flight-plan, filing-status, flight-arrival, flight-cancellation, flight-departure, flight-plan-update, planning-status.

## Mandatory Standards

- **JSON in shell**: `jq` only (no Python/Node)
- **K8s resources**: YAML files only, apply with `oc apply -f`
- **Consumer ↔ Validator rule**: A consumer never connects to its own provider — it always connects to the consumer-validator

## Configuration (application.properties)

Key properties configurable via environment variables:
- `AMQP_BROKER_HOST/PORT/USERNAME/PASSWORD` — Artemis connection
- `MARIADB_HOST/PORT/DATABASE/USERNAME/PASSWORD` — MariaDB connection
- `EVENT_GENERATOR_ENABLED/SCHEDULE/EVENTS_PATH` — event publishing
- `HEARTBEAT_PUBLISHER_ENABLED/INTERVAL` — heartbeat
- Default topic: `ffice.v1`, queue prefix: `FFICE.v1`
