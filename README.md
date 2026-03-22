# DankPoster

Reactive content ingestion & distribution platform built with Spring Boot.

DankPoster ingests media content from external APIs and delivers it through a controlled, failure-aware publishing pipeline.

Designed as a production-ready foundation for event-driven delivery systems.

---

## Core Capabilities

- External source ingestion (Reddit / Giphy)
- Reactive processing pipeline
- Idempotent persistence layer
- Delivery retry handling
- Business-driven rate limiting
- Failure state tracking

---

## Architecture

DankPoster follows a pipeline-based architecture:

Source -> Persistence -> Delivery

Key design principles:

- Source abstraction (pluggable providers)
- Idempotent storage
- Backpressure-aware reactive flow
- Delivery isolation
- Fault tolerance

---

## Tech Stack

- Java 17
- Spring Boot 3
- Spring WebFlux
- Spring Data JPA
- PostgreSQL-ready
- Dockerized

---

## System Design Considerations

- External API rate limit resilience
- Business rule-based delivery intervals
- Failure-aware processing
- Retry-ready delivery layer
- Deduplication via persistence

---

## Deployment

Containerized via Docker and ready for cloud environments (AWS compatible).

---

## Roadmap

- Queue decoupling (SQS-ready)
- Distributed scheduling
- Multi-channel delivery
- Observability improvements

---

## Author

Built as a product-oriented backend system focusing on reliability and controlled delivery pipelines.
