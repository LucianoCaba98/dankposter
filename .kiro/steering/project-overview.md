---
inclusion: always
---

# DankPoster — Project Overview

DankPoster is a reactive meme ingestion and distribution pipeline built with Spring Boot 3.5 and Java 17.
It fetches memes from Reddit and Giphy, persists them with deduplication, and posts them to a Discord channel on a timed interval.

## Architecture

Source → Persistence → Delivery

- Sources are pluggable via the `MemeSource` interface and conditionally loaded with `@ConditionalOnProperty`
- Persistence uses Spring Data JPA with H2 (dev) / PostgreSQL (prod-ready)
- Delivery uses Discord Bot API via WebClient with source-specific renderers (Strategy pattern)

## Key Packages

- `com.dankposter.config` — Spring configuration and properties binding
- `com.dankposter.model` — JPA entities, enums, domain interfaces
- `com.dankposter.dto` — API response DTOs for Reddit, Giphy, Discord
- `com.dankposter.service` — Core pipeline orchestration and posting
- `com.dankposter.externalIntegrations` — Source fetchers and Discord renderers
- `com.dankposter.repository` — Spring Data JPA repositories

## Tech Stack

- Java 17, Spring Boot 3.5, Spring WebFlux (reactive WebClient)
- Spring Data JPA, H2 / PostgreSQL
- Lombok, Project Reactor
- Docker (Alpine JRE 17)

## Environment Variables

All secrets and configuration are injected via environment variables. See `.ENV` for the template.
Key variables: `DISCORD_BOT_TOKEN`, `DISCORD_CHANNEL_ID`, `GIPHY_API_KEY`, `SR_1`..`SR_10` (subreddit names).
