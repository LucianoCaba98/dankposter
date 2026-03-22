# Contributing to DankPoster

## Project Structure

```
src/main/java/com/dankposter/
├── config/                  # Spring configuration & properties binding
├── dto/                     # API response DTOs (reddit/, giphy/)
├── externalIntegrations/    # Source fetchers & Discord renderers
│   ├── discord/render/      # Strategy-based meme rendering
│   ├── giphy/               # Giphy trending GIF fetcher
│   └── reddit/              # Reddit hot post fetcher
├── model/                   # JPA entities, enums, interfaces
│   └── error/               # Custom exceptions
├── repository/              # Spring Data JPA repositories
└── service/                 # Pipeline orchestration & posting
```

## Adding a New Meme Source

1. Create a DTO package under `dto/` for the API response models
2. Implement `MemeSource` interface in a new package under `externalIntegrations/`
3. Annotate with `@ConditionalOnProperty(prefix = "meme.sources", name = "yoursource", havingValue = "true")`
4. Add a `MemeRenderer` implementation under `externalIntegrations/discord/render/`
5. Add the new `Source` enum value
6. Add configuration to `application.yml`

## Adding a New Delivery Channel

1. Create a new service under `service/` (similar to `DiscordPosterService`)
2. Create DTOs for the channel's API under `externalIntegrations/`
3. Wire it into `MemePipeline`

## Environment Setup

1. Copy `.ENV` and fill in your values
2. Required variables:
   - `DISCORD_BOT_TOKEN` — Discord bot token
   - `DISCORD_CHANNEL_ID` — Target channel ID
   - `GIPHY_API_KEY` — Giphy API key
   - `SR_1` through `SR_10` — Subreddit names
3. Optional:
   - `PORT` (default: 8080)
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (default: H2 in-memory)
   - `DDL_AUTO` (default: update)
   - `MEME_SOURCES_REDDIT` / `MEME_SOURCES_GIPHY` (default: reddit=false, giphy=true)

## Code Style

- Use Lombok annotations consistently (`@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`)
- All external I/O must be reactive (Mono/Flux)
- Wrap blocking JPA calls with `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`
- Handle errors explicitly — no silent swallowing
- Log with context (meme ID, source name)

## Testing

- Mirror the main source structure under `src/test/java/com/dankposter/`
- Use `StepVerifier` for reactive chain testing
- Mock external APIs with WireMock or `@MockBean`
