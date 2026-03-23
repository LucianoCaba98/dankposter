# DankPoster — Quickstart Guide

## Prerequisites

- Java 17+
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- Node.js 18+ and npm
- A Discord bot token and channel ID ([create one here](https://discord.com/developers/applications))

## 1. Clone and configure

```bash
git clone <your-repo-url>
cd dankposter
```

Copy the `.ENV` file and add your secrets:

```bash
cp .ENV .env.local
```

Edit `.env.local` and set these required values:

```dotenv
# Required — Discord
DISCORD_BOT_TOKEN=your-bot-token-here
DISCORD_CHANNEL_ID=your-channel-id-here

# Subreddits — defaults are set in application.yml, override here if you want
SR_1=dankmemes
SR_2=memes
# ... up to SR_20

# Optional — Giphy (enables Giphy as a meme source)
GIPHY_API_KEY=your-giphy-key

# Optional — override defaults
PORT=8080
```

> SQS and Kafka are disabled by default. See `SQS-KAFKA-PLAYBOOK.md` to enable them.

## 2. Architecture overview

DankPoster uses a fully reactive pipeline — no schedulers or cron jobs.

```
Sources (Reddit, Giphy) → MemePipeline → Persistence (H2/PostgreSQL) → Discord Delivery
```

- **`MemePipeline`** — a `@PostConstruct` reactive `Flux.interval` chain that fetches, persists, and posts memes
- **`MemeSource` interface** — pluggable sources (`RedditMemeSource`, `GiphyMemeSource`) loaded via `@ConditionalOnProperty`
- **`MemeRenderer` strategy** — source-specific Discord embed rendering (`RedditMemeRenderer`, `GiphyMemeRenderer`)
- **SSE events** — real-time ingestion/posted events pushed to the frontend
- **SQS/Kafka** — optional message queue and event streaming integration (disabled by default)

### Package structure

```
com.dankposter
├── config/              # Spring config, properties binding
├── controller/          # REST + SSE endpoints
├── dto/                 # Reddit, Giphy, Kafka DTOs
├── externalIntegrations/
│   ├── discord/         # Discord renderers (Strategy pattern)
│   ├── giphy/           # GiphyMemeSource
│   └── reddit/          # RedditMemeSource
├── model/               # JPA entities, enums, MemeSource interface
├── repository/          # Spring Data JPA
└── service/             # Pipeline, Discord poster, SSE, SQS, Kafka
```

## 3. Start the backend

Export your env vars and run:

```bash
# Linux/macOS
export $(cat .env.local | xargs)
./mvnw spring-boot:run
```

```powershell
# Windows (PowerShell)
Get-Content .ENV | ForEach-Object {
  if ($_ -match '^([^#].+?)=(.*)$') {
    [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
  }
}
./mvnw.cmd spring-boot:run
```

The backend starts on `http://localhost:8080` with an in-memory H2 database (no setup needed).

On startup, the reactive pipeline immediately begins:
- Fetching memes from all configured sources every 5 minutes
- Deduplicating by `externalId` (Reddit post ID, Giphy GIF ID)
- Posting to Discord with a 30-second delay between posts
- Broadcasting SSE events for real-time frontend updates

## 4. Start the frontend

In a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

> **Windows note:** If `npm` fails due to PowerShell execution policy, use `cmd /c "cd frontend && npx vite --host"` instead.

The Vite dev server starts on `http://localhost:5173` and proxies `/api` requests to the backend.

## 5. Open the app

Navigate to `http://localhost:5173`:

| Route | What it shows |
|-------|--------------|
| `/` | Posted memes — successfully sent to Discord, no status badges |
| `/#/ingestion` | All memes with Posted/Pending status, live counters via SSE |
| `/#/admin` | Admin panel — configure subreddits, Discord, scheduling, SQS, Kafka |

Click any meme card to expand it (Instagram-style overlay).

## 6. Meme sources

| Source | Enabled by | How it works |
|--------|-----------|--------------|
| Reddit | `meme.sources.reddit=true` | Fetches from configured subreddits via public `.json` API. No API key needed. |
| Giphy | `meme.sources.giphy=true` | Fetches trending GIFs. Requires `GIPHY_API_KEY`. |

Both sources are toggled via `application.yml` or env vars (`MEME_SOURCES_REDDIT`, `MEME_SOURCES_GIPHY`).

## 7. Build for production

Bundle the frontend into Spring Boot's static resources:

```bash
cd frontend
npm run build
```

Then package everything into a single JAR:

```bash
./mvnw clean package -DskipTests
java -jar target/dank-1.0.0.jar
```

### Docker

```bash
./mvnw clean package -DskipTests
docker build -t dankposter .
docker run --env-file .ENV -p 8080:8080 dankposter
```

## 8. Database options

| Mode | Config | Notes |
|------|--------|-------|
| Dev (default) | H2 in-memory | Zero setup, data resets on restart |
| Prod | PostgreSQL | Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` env vars |

DDL strategy is controlled by `DDL_AUTO` env var (default: `update`).

## 9. Optional integrations

### SQS (message queue)

Set `SQS_ENABLED=true` plus `SQS_QUEUE_URL`, `SQS_DLQ_URL`, `AWS_REGION`. When enabled, fetched memes are sent to SQS instead of being persisted directly. An SQS consumer polls and persists them.

### Kafka (event streaming)

Set `KAFKA_ENABLED=true` plus `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_TOPIC`, `KAFKA_CONSUMER_GROUP`. When enabled, delivery events are published to Kafka after persistence. A Kafka consumer handles Discord posting instead of the pipeline.

See `SQS-KAFKA-PLAYBOOK.md` for full setup instructions.

## Troubleshooting

- **No memes appearing?** Check that at least one source is enabled (`meme.sources.reddit=true` or `meme.sources.giphy=true` in `application.yml`). Check backend logs for fetch errors.
- **Discord posting fails?** Verify `DISCORD_BOT_TOKEN` and `DISCORD_CHANNEL_ID`. Make sure the bot has "Send Messages" and "Embed Links" permissions in the channel.
- **Frontend can't reach API?** Ensure the backend is running on port 8080. The Vite proxy forwards `/api` there.
- **Reddit rate limiting?** The app handles per-subreddit failures gracefully — one failing subreddit won't kill the batch. If you see many `Connection reset` warnings, reduce the number of subreddits or increase the fetch interval.
- **Title too long errors?** Fixed — the `title` column supports up to 500 characters and titles are truncated at the fetcher level.
- **H2 console?** Disabled by default. Set `spring.h2.console.enabled=true` in `application.yml` if needed.
