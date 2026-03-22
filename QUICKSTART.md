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

# Required — at least one subreddit (defaults are already set in .ENV)
SR_1=dankmemes
SR_2=memes
# ... SR_3 through SR_10 are optional

# Optional — Giphy (if you want Giphy as a source)
GIPHY_API_KEY=your-giphy-key

# Optional — override defaults
PORT=8080
SCHEDULING_FETCH_INTERVAL_MS=300000   # fetch memes every 5 min
SCHEDULING_POST_INTERVAL_MS=30000     # post to Discord every 30 sec
```

> SQS and Kafka are disabled by default. See `SQS-KAFKA-PLAYBOOK.md` if you want to enable them.

## 2. Start the backend

Export your env vars and run:

```bash
# Linux/macOS
export $(cat .env.local | xargs)
./mvnw spring-boot:run

# Windows (PowerShell)
Get-Content .env.local | ForEach-Object {
  if ($_ -match '^([^#].+?)=(.*)$') {
    [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
  }
}
./mvnw.cmd spring-boot:run
```

The backend starts on `http://localhost:8080`. It uses an in-memory H2 database by default — no DB setup needed.

Once running, the scheduler will:
- Fetch memes from your configured subreddits every 5 minutes
- Post the dankest unposted meme to Discord every 30 seconds

## 3. Start the frontend

In a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server starts on `http://localhost:5173` and proxies `/api` requests to the backend on port 8080.

## 4. Open the app

Navigate to `http://localhost:5173` in your browser. You'll see three pages:

| Route | What it shows |
|-------|--------------|
| `/` | Posted Memes — memes that have been sent to Discord |
| `/#/ingestion` | Ingestion Feed — all memes (posted + pending), live-updating via SSE |
| `/#/admin` | Admin Panel — configure scheduling, Discord, subreddits, SQS, Kafka |

> The app uses hash-based routing (`/#/`), so all routes work without server-side config.

## 5. Build for production (optional)

To bundle the frontend into the Spring Boot static resources:

```bash
cd frontend
npm run build
```

This outputs to `src/main/resources/static/`. Then package everything:

```bash
./mvnw clean package -DskipTests
java -jar target/dank-0.0.1-SNAPSHOT.jar
```

Now the entire app (backend + frontend) runs from a single JAR on port 8080.

## Database options

| Mode | Config | Notes |
|------|--------|-------|
| Dev (default) | H2 in-memory | Zero setup, data resets on restart |
| Prod | PostgreSQL | Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` env vars |

## Troubleshooting

- **No memes appearing?** Check that your subreddit env vars (`SR_1`, etc.) are set and the backend logs show successful Reddit fetches.
- **Discord posting fails?** Verify `DISCORD_BOT_TOKEN` and `DISCORD_CHANNEL_ID`. Make sure the bot has "Send Messages" permission in the channel.
- **Frontend can't reach API?** Ensure the backend is running on port 8080. The Vite proxy forwards `/api` there.
- **H2 console?** Disabled by default. Set `spring.h2.console.enabled=true` in `application.yml` if you need it.
