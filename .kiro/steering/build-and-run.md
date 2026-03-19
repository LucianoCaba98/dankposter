---
inclusion: always
---

# Build & Run Instructions

## Prerequisites

- Java 17+
- Maven 3.9+ (or use the included `mvnw` wrapper)
- Docker (optional, for containerized runs)

## Build

```bash
./mvnw clean package -DskipTests
```

## Run Locally

1. Copy `.ENV` and fill in your actual values for `DISCORD_BOT_TOKEN`, `DISCORD_CHANNEL_ID`, `GIPHY_API_KEY`
2. Export the env vars or use your IDE's run configuration
3. Run:

```bash
./mvnw spring-boot:run
```

The app starts on port 8080 (configurable via `PORT` env var). The pipeline kicks off automatically via `@PostConstruct`.

## Run with Docker

```bash
./mvnw clean package -DskipTests
docker build -t dankposter .
docker run --env-file .ENV -p 8080:8080 dankposter
```

## Run Tests

```bash
./mvnw test
```

## Database

- Dev: H2 in-memory (default, no setup needed)
- Prod: Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` for PostgreSQL
- DDL strategy controlled by `DDL_AUTO` env var (default: `update`)
