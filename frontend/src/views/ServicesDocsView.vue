<template>
  <div class="services-docs">
    <div class="docs-content">
      <h2 class="page-title">Services</h2>

      <!-- Scrollable Tab Bar -->
      <div class="tab-bar">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="tab-button"
          :class="{ active: activeTab === tab.id }"
          @click="switchTab(tab.id)"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- MemePipeline -->
      <div v-if="activeTab === 'meme-pipeline'">
        <DocsSection
          title="Overview"
          section-id="meme-pipeline-overview"
          :expanded="expandedSections.has('meme-pipeline-overview')"
          @toggle="toggleSection"
        >
          <p>
            <strong>MemePipeline</strong> is the central orchestrator of the entire meme ingestion
            and delivery lifecycle. It runs automatically when the application starts via
            <code>@EventListener(ApplicationReadyEvent.class)</code> and coordinates fetching,
            persistence, intercalation, and Kafka publishing in a single reactive pipeline.
          </p>
          <p>
            The pipeline executes a fetch cycle every <strong>30 minutes</strong>, deduplicates
            memes against the database, intercalates them by source for variety, then delivers
            each meme to Kafka with a <strong>20-second</strong> delay between publishes.
          </p>
        </DocsSection>

        <DocsSection
          title="Key Implementation Details"
          section-id="meme-pipeline-details"
          :expanded="expandedSections.has('meme-pipeline-details')"
          @toggle="toggleSection"
        >
          <p>The pipeline is built as a single reactive chain using Project Reactor:</p>
          <ul>
            <li><code>Flux.interval(Duration.ZERO, Duration.ofMinutes(30))</code> triggers fetch cycles, starting immediately on boot</li>
            <li><code>onBackpressureDrop</code> skips overlapping ticks if a previous cycle is still running</li>
            <li>Each meme is saved via <code>Mono.fromCallable(() → memeRepository.save(meme))</code> on <code>Schedulers.boundedElastic()</code> to avoid blocking the reactive thread</li>
            <li>Duplicates are silently dropped using <code>onErrorResume(DataIntegrityViolationException.class, ...)</code></li>
            <li>After collecting the batch, <code>MemeIntercalator.intercalate()</code> applies round-robin source ordering</li>
            <li>Delivery uses <code>concatMap</code> with <code>Mono.delay(Duration.ofSeconds(20))</code> for sequential, timed publishing to Kafka</li>
          </ul>
        </DocsSection>

        <DocsSection
          title="Dependencies &amp; Interactions"
          section-id="meme-pipeline-deps"
          :expanded="expandedSections.has('meme-pipeline-deps')"
          @toggle="toggleSection"
        >
          <ul>
            <li><strong>MemeFetchService</strong> — aggregates memes from all registered sources</li>
            <li><strong>MemeRepository</strong> — persists memes with deduplication via JPA</li>
            <li><strong>MemeIntercalator</strong> — static utility for round-robin source ordering</li>
            <li><strong>KafkaProducerService</strong> — publishes <code>MemeDeliveryEvent</code> to Kafka (injected directly, always-on)</li>
            <li><strong>MemeEventPublisher</strong> — broadcasts SSE ingestion events after saving</li>
          </ul>
        </DocsSection>
      </div>

      <!-- MemeFetchService -->
      <div v-if="activeTab === 'meme-fetch-service'">
        <DocsSection
          title="Overview"
          section-id="meme-fetch-overview"
          :expanded="expandedSections.has('meme-fetch-overview')"
          @toggle="toggleSection"
        >
          <p>
            <strong>MemeFetchService</strong> aggregates all registered <code>MemeSource</code>
            implementations and fetches memes from each one concurrently. It acts as the single
            entry point for the fetch phase of the pipeline.
          </p>
          <p>
            On startup, it logs all registered source names via <code>@PostConstruct</code>,
            providing visibility into which sources are active for the current configuration.
          </p>
        </DocsSection>

        <DocsSection
          title="Key Implementation Details"
          section-id="meme-fetch-details"
          :expanded="expandedSections.has('meme-fetch-details')"
          @toggle="toggleSection"
        >
          <ul>
            <li>Receives <code>List&lt;MemeSource&gt;</code> via constructor injection — Spring auto-discovers all <code>MemeSource</code> beans</li>
            <li>Uses <code>Flux.fromIterable(sources).flatMap(source → source.fetch())</code> for concurrent fetching across all sources</li>
            <li>Each source returns its own <code>Flux&lt;Meme&gt;</code>, which are merged into a single stream</li>
            <li>Error handling is delegated to individual source implementations</li>
          </ul>
        </DocsSection>

        <DocsSection
          title="Dependencies &amp; Interactions"
          section-id="meme-fetch-deps"
          :expanded="expandedSections.has('meme-fetch-deps')"
          @toggle="toggleSection"
        >
          <ul>
            <li><strong>MemeSource</strong> interface — all implementations (RedditMemeSource, GiphyMemeSource) are injected as a list</li>
            <li>Called by <strong>MemePipeline</strong> during each fetch cycle</li>
            <li>Does not interact with the database — persistence is handled downstream by the pipeline</li>
          </ul>
        </DocsSection>
      </div>

      <!-- RedditMemeSource -->
      <div v-if="activeTab === 'reddit-meme-source'">
        <DocsSection
          title="Overview"
          section-id="reddit-overview"
          :expanded="expandedSections.has('reddit-overview')"
          @toggle="toggleSection"
        >
          <p>
            <strong>RedditMemeSource</strong> implements the <code>MemeSource</code> interface
            and fetches top image posts from configured subreddits via Reddit's public JSON API.
            It is conditionally loaded with <code>@ConditionalOnProperty(prefix = "meme.sources", name = "reddit", havingValue = "true")</code>.
          </p>
          <p>
            For each subreddit, it fetches both <strong>hot</strong> and <strong>new</strong> sort modes,
            with a 1.5-second delay between requests to respect Reddit's rate limits.
          </p>
        </DocsSection>

        <DocsSection
          title="Key Implementation Details"
          section-id="reddit-details"
          :expanded="expandedSections.has('reddit-details')"
          @toggle="toggleSection"
        >
          <ul>
            <li>Iterates over configured subreddits from <code>RedditProperties</code>, each with a configurable <code>limit</code></li>
            <li>Uses <code>concatMap</code> for sequential subreddit fetching with <code>Mono.delay(1500ms)</code> rate limiting</li>
            <li>Filters posts by <code>post_hint: "image"</code> or direct media URL extensions (.jpg, .png, .gif, .gifv)</li>
            <li>Truncates titles longer than 500 characters</li>
            <li>Sets <code>User-Agent: DankPoster/1.0</code> header on all requests</li>
            <li>Errors per subreddit/mode are logged and swallowed — one failing subreddit does not block others</li>
          </ul>
        </DocsSection>

        <DocsSection
          title="Dependencies &amp; Interactions"
          section-id="reddit-deps"
          :expanded="expandedSections.has('reddit-deps')"
          @toggle="toggleSection"
        >
          <ul>
            <li><strong>WebClient</strong> — makes HTTP GET requests to Reddit's JSON API</li>
            <li><strong>RedditProperties</strong> — provides the list of subreddits and their fetch limits</li>
            <li>Discovered by <strong>MemeFetchService</strong> via Spring's <code>List&lt;MemeSource&gt;</code> injection</li>
          </ul>
        </DocsSection>
      </div>

      <!-- GiphyMemeSource -->
      <div v-if="activeTab === 'giphy-meme-source'">
        <DocsSection
          title="Overview"
          section-id="giphy-overview"
          :expanded="expandedSections.has('giphy-overview')"
          @toggle="toggleSection"
        >
          <p>
            <strong>GiphyMemeSource</strong> implements the <code>MemeSource</code> interface
            and fetches trending GIFs from the Giphy API. It is conditionally loaded with
            <code>@ConditionalOnProperty(prefix = "meme.sources", name = "giphy", havingValue = "true")</code>.
          </p>
          <p>
            It fetches up to 50 trending GIFs per cycle, filtered to PG rating and Spanish language locale.
          </p>
        </DocsSection>

        <DocsSection
          title="Key Implementation Details"
          section-id="giphy-details"
          :expanded="expandedSections.has('giphy-details')"
          @toggle="toggleSection"
        >
          <ul>
            <li>Uses a dedicated <code>giphyClient</code> WebClient bean with the API key pre-configured</li>
            <li>Calls <code>/v1/gifs/trending</code> with <code>limit=50</code>, <code>rating=pg</code>, <code>lang=es</code></li>
            <li>Resolves GIF URLs with a fallback chain: <code>downsized</code> → <code>original</code> → <code>fixed_height</code></li>
            <li>GIFs with no usable URL are filtered out with a warning log</li>
            <li>External IDs are prefixed with <code>giphy_</code> for deduplication</li>
            <li>Errors are logged and swallowed — a Giphy API failure does not crash the pipeline</li>
          </ul>
        </DocsSection>

        <DocsSection
          title="Dependencies &amp; Interactions"
          section-id="giphy-deps"
          :expanded="expandedSections.has('giphy-deps')"
          @toggle="toggleSection"
        >
          <ul>
            <li><strong>giphyClient</strong> (WebClient) — pre-configured with Giphy API base URL and key</li>
            <li>Discovered by <strong>MemeFetchService</strong> via Spring's <code>List&lt;MemeSource&gt;</code> injection</li>
          </ul>
        </DocsSection>
      </div>

      <!-- DiscordPosterService -->
      <div v-if="activeTab === 'discord-poster-service'">
        <DocsSection
          title="Overview"
          section-id="discord-overview"
          :expanded="expandedSections.has('discord-overview')"
          @toggle="toggleSection"
        >
          <p>
            <strong>DiscordPosterService</strong> posts memes to a Discord channel via the
            Discord Bot API using a reactive <code>WebClient</code>. It delegates message
            formatting to <code>MemeRenderService</code>, which uses the Strategy pattern
            to select source-specific renderers.
          </p>
          <p>
            The <code>post()</code> method returns a <code>Mono&lt;Meme&gt;</code>, making it
            composable within the reactive pipeline.
          </p>
        </DocsSection>

        <DocsSection
          title="Key Implementation Details"
          section-id="discord-details"
          :expanded="expandedSections.has('discord-details')"
          @toggle="toggleSection"
        >
          <ul>
            <li>Uses <code>MemeRenderService.render(meme)</code> to produce a <code>DiscordMessagePayload</code> with embeds tailored to the meme's source</li>
            <li>Posts to <code>/channels/{channelId}/messages</code> using the configured <code>discordClient</code> WebClient</li>
            <li>The channel ID comes from <code>DiscordConfig</code> (bound to environment variables)</li>
            <li>Returns the original <code>Meme</code> object on success via <code>thenReturn(meme)</code></li>
          </ul>
        </DocsSection>

        <DocsSection
          title="Dependencies &amp; Interactions"
          section-id="discord-deps"
          :expanded="expandedSections.has('discord-deps')"
          @toggle="toggleSection"
        >
          <ul>
            <li><strong>DiscordConfig</strong> — provides the bot token and channel ID</li>
            <li><strong>discordClient</strong> (WebClient) — pre-configured with Discord API base URL and auth headers</li>
            <li><strong>MemeRenderService</strong> — selects the appropriate <code>MemeRenderer</code> (GiphyMemeRenderer or RedditMemeRenderer) based on the meme's source</li>
            <li>Called by <strong>KafkaConsumerService</strong> after consuming a delivery event from Kafka</li>
          </ul>
        </DocsSection>
      </div>

      <!-- KafkaProducerService -->
      <div v-if="activeTab === 'kafka-producer-service'">
        <DocsSection
          title="Overview"
          section-id="kafka-producer-overview"
          :expanded="expandedSections.has('kafka-producer-overview')"
          @toggle="toggleSection"
        >
          <p>
            <strong>KafkaProducerService</strong> publishes <code>MemeDeliveryEvent</code>
            messages to the Kafka <code>meme-delivery</code> topic. It is a permanent, always-on
            service — no feature toggle required.
          </p>
          <p>
            Messages are keyed by <code>memeId</code> to ensure partition ordering for the same meme.
          </p>
        </DocsSection>

        <DocsSection
          title="Key Implementation Details"
          section-id="kafka-producer-details"
          :expanded="expandedSections.has('kafka-producer-details')"
          @toggle="toggleSection"
        >
          <ul>
            <li>Uses <code>KafkaTemplate&lt;String, String&gt;</code> for async message publishing</li>
            <li>Serializes <code>MemeDeliveryEvent</code> to JSON via <code>ObjectMapper</code></li>
            <li>The <code>send()</code> call uses a <code>whenComplete</code> callback to handle success/failure asynchronously</li>
            <li>On successful send, publishes a "produced" metric via <code>KafkaMetricsPublisher</code></li>
            <li>Message key is <code>String.valueOf(event.memeId())</code> — ensures all events for the same meme land on the same partition</li>
            <li>Serialization failures are caught and logged without propagating</li>
          </ul>
        </DocsSection>

        <DocsSection
          title="Dependencies &amp; Interactions"
          section-id="kafka-producer-deps"
          :expanded="expandedSections.has('kafka-producer-deps')"
          @toggle="toggleSection"
        >
          <ul>
            <li><strong>KafkaTemplate</strong> — Spring Kafka's producer abstraction</li>
            <li><strong>KafkaProperties</strong> — provides the topic name</li>
            <li><strong>ObjectMapper</strong> — JSON serialization of delivery events</li>
            <li><strong>KafkaMetricsPublisher</strong> — broadcasts "produced" SSE events on successful publish</li>
            <li>Called by <strong>MemePipeline</strong> during the delivery phase</li>
          </ul>
        </DocsSection>
      </div>

      <!-- KafkaConsumerService -->
      <div v-if="activeTab === 'kafka-consumer-service'">
        <DocsSection
          title="Overview"
          section-id="kafka-consumer-overview"
          :expanded="expandedSections.has('kafka-consumer-overview')"
          @toggle="toggleSection"
        >
          <p>
            <strong>KafkaConsumerService</strong> listens on the <code>meme-delivery</code> Kafka
            topic using <code>@KafkaListener</code> and delivers memes to Discord. It uses manual
            acknowledgment and implements idempotent delivery by checking the meme's status before posting.
          </p>
          <p>
            This is the consumer side of the Kafka delivery pipeline — it bridges the gap between
            the Kafka topic and the Discord Bot API.
          </p>
        </DocsSection>

        <DocsSection
          title="Key Implementation Details"
          section-id="kafka-consumer-details"
          :expanded="expandedSections.has('kafka-consumer-details')"
          @toggle="toggleSection"
        >
          <ul>
            <li>Uses <code>@KafkaListener</code> with SpEL expressions for topic and group ID from <code>KafkaProperties</code></li>
            <li>Manual <code>Acknowledgment</code> — messages are only acknowledged after successful processing or when skipped</li>
            <li>Deserializes <code>ConsumerRecord&lt;String, String&gt;</code> into <code>MemeDeliveryEvent</code> via <code>ObjectMapper</code></li>
            <li>Idempotent delivery: checks if <code>meme.getStatus() == MemeStatus.POSTED</code> and skips already-posted memes</li>
            <li>On successful Discord post: updates meme status to <code>POSTED</code>, saves to DB, and publishes a posted SSE event</li>
            <li>Publishes Kafka lifecycle metrics at each stage: consumed, delivered, or failed</li>
          </ul>
        </DocsSection>

        <DocsSection
          title="Dependencies &amp; Interactions"
          section-id="kafka-consumer-deps"
          :expanded="expandedSections.has('kafka-consumer-deps')"
          @toggle="toggleSection"
        >
          <ul>
            <li><strong>ObjectMapper</strong> — deserializes JSON payloads from Kafka records</li>
            <li><strong>MemeRepository</strong> — looks up memes by ID and persists status updates</li>
            <li><strong>DiscordPosterService</strong> — posts the meme to Discord (called with <code>.block()</code>)</li>
            <li><strong>MemeEventPublisher</strong> — broadcasts "posted" SSE events after successful delivery</li>
            <li><strong>KafkaMetricsPublisher</strong> — broadcasts consumed/delivered/failed lifecycle metrics</li>
          </ul>
        </DocsSection>
      </div>

      <!-- KafkaMetricsPublisher -->
      <div v-if="activeTab === 'kafka-metrics-publisher'">
        <DocsSection
          title="Overview"
          section-id="kafka-metrics-overview"
          :expanded="expandedSections.has('kafka-metrics-overview')"
          @toggle="toggleSection"
        >
          <p>
            <strong>KafkaMetricsPublisher</strong> broadcasts Kafka lifecycle events to the frontend
            in real time via Server-Sent Events. It tracks four stages of the delivery lifecycle:
            <strong>produced</strong>, <strong>consumed</strong>, <strong>delivered</strong>, and <strong>failed</strong>.
          </p>
          <p>
            These metrics power the Kafka Metrics tab in the Admin Panel, giving operators
            live visibility into the delivery pipeline's health.
          </p>
        </DocsSection>

        <DocsSection
          title="Key Implementation Details"
          section-id="kafka-metrics-details"
          :expanded="expandedSections.has('kafka-metrics-details')"
          @toggle="toggleSection"
        >
          <ul>
            <li>Constructs <code>KafkaMessageEvent</code> records with topic, partition, offset, key, payload, delivery status, and timestamp</li>
            <li>Uses <code>SseEmitterService.broadcast("kafka-metrics", event)</code> to push events to all connected SSE clients</li>
            <li>The <code>broadcastSafely()</code> wrapper catches and logs any SSE broadcast failures without propagating them</li>
            <li>Called by both <code>KafkaProducerService</code> (produced) and <code>KafkaConsumerService</code> (consumed, delivered, failed)</li>
          </ul>
        </DocsSection>

        <DocsSection
          title="Dependencies &amp; Interactions"
          section-id="kafka-metrics-deps"
          :expanded="expandedSections.has('kafka-metrics-deps')"
          @toggle="toggleSection"
        >
          <ul>
            <li><strong>SseEmitterService</strong> — manages the <code>kafka-metrics</code> SSE channel and broadcasts events to connected clients</li>
            <li>Called by <strong>KafkaProducerService</strong> on successful publish and by <strong>KafkaConsumerService</strong> at each lifecycle stage</li>
            <li>Frontend consumers: <strong>MetricsPanel</strong> and <strong>MessageInspector</strong> in the Admin Panel's Kafka Metrics tab</li>
          </ul>
        </DocsSection>
      </div>

      <!-- MemeEventPublisher -->
      <div v-if="activeTab === 'meme-event-publisher'">
        <DocsSection
          title="Overview"
          section-id="meme-event-overview"
          :expanded="expandedSections.has('meme-event-overview')"
          @toggle="toggleSection"
        >
          <p>
            <strong>MemeEventPublisher</strong> publishes SSE events for meme ingestion and posting.
            It converts <code>Meme</code> entities to <code>MemeDto</code> before broadcasting,
            ensuring the frontend receives a clean, serializable representation.
          </p>
          <p>
            It powers the real-time feeds on the Ingestion page and the Live Meme Feed sidebar.
          </p>
        </DocsSection>

        <DocsSection
          title="Key Implementation Details"
          section-id="meme-event-details"
          :expanded="expandedSections.has('meme-event-details')"
          @toggle="toggleSection"
        >
          <ul>
            <li><code>publishIngested(List&lt;Meme&gt;)</code> — iterates over newly saved memes and broadcasts each as a <code>MemeDto</code> on the <code>ingestion</code> SSE channel</li>
            <li><code>publishPosted(Meme)</code> — broadcasts a single meme on the <code>posted</code> SSE channel after successful Discord delivery</li>
            <li>Uses <code>MemeDto.fromEntity(meme)</code> for entity-to-DTO conversion</li>
          </ul>
        </DocsSection>

        <DocsSection
          title="Dependencies &amp; Interactions"
          section-id="meme-event-deps"
          :expanded="expandedSections.has('meme-event-deps')"
          @toggle="toggleSection"
        >
          <ul>
            <li><strong>SseEmitterService</strong> — manages the <code>ingestion</code> and <code>posted</code> SSE channels</li>
            <li>Called by <strong>MemePipeline</strong> after saving new memes (ingestion events)</li>
            <li>Called by <strong>KafkaConsumerService</strong> after successful Discord delivery (posted events)</li>
            <li>Frontend consumers: <strong>IngestionFeed</strong>, <strong>LiveMemeFeed</strong>, and <strong>HomeFeed</strong></li>
          </ul>
        </DocsSection>
      </div>

      <!-- SseEmitterService -->
      <div v-if="activeTab === 'sse-emitter-service'">
        <DocsSection
          title="Overview"
          section-id="sse-emitter-overview"
          :expanded="expandedSections.has('sse-emitter-overview')"
          @toggle="toggleSection"
        >
          <p>
            <strong>SseEmitterService</strong> manages Server-Sent Event channels for real-time
            communication between the backend and frontend. It maintains three channels:
            <code>ingestion</code>, <code>posted</code>, and <code>kafka-metrics</code>.
          </p>
          <p>
            Each channel supports multiple concurrent SSE clients, with automatic cleanup
            on completion, timeout, or error.
          </p>
        </DocsSection>

        <DocsSection
          title="Key Implementation Details"
          section-id="sse-emitter-details"
          :expanded="expandedSections.has('sse-emitter-details')"
          @toggle="toggleSection"
        >
          <ul>
            <li>Uses <code>ConcurrentHashMap&lt;String, CopyOnWriteArrayList&lt;SseEmitter&gt;&gt;</code> for thread-safe channel management</li>
            <li>Channels are pre-registered in the constructor: <code>ingestion</code>, <code>posted</code>, <code>kafka-metrics</code></li>
            <li><code>createEmitter(channel)</code> creates a new <code>SseEmitter</code> with a 30-minute timeout (1,800,000ms)</li>
            <li>Emitter lifecycle callbacks (<code>onCompletion</code>, <code>onTimeout</code>, <code>onError</code>) automatically remove dead emitters from the channel list</li>
            <li><code>broadcast(channel, data)</code> iterates over all emitters in a channel and sends the data; failed emitters are removed on <code>IOException</code></li>
          </ul>
        </DocsSection>

        <DocsSection
          title="Dependencies &amp; Interactions"
          section-id="sse-emitter-deps"
          :expanded="expandedSections.has('sse-emitter-deps')"
          @toggle="toggleSection"
        >
          <ul>
            <li><strong>SseController</strong> — exposes <code>/api/events/{channel}</code> endpoints that call <code>createEmitter()</code></li>
            <li><strong>MemeEventPublisher</strong> — broadcasts to <code>ingestion</code> and <code>posted</code> channels</li>
            <li><strong>KafkaMetricsPublisher</strong> — broadcasts to the <code>kafka-metrics</code> channel</li>
            <li>No external dependencies — this is a standalone infrastructure service</li>
          </ul>
        </DocsSection>
      </div>
    </div>

    <div class="feed-sidebar">
      <LiveMemeFeed />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import DocsSection from '../components/DocsSection.vue'
import LiveMemeFeed from '../components/LiveMemeFeed.vue'

const tabs = [
  { id: 'meme-pipeline', label: 'MemePipeline' },
  { id: 'meme-fetch-service', label: 'MemeFetchService' },
  { id: 'reddit-meme-source', label: 'RedditMemeSource' },
  { id: 'giphy-meme-source', label: 'GiphyMemeSource' },
  { id: 'discord-poster-service', label: 'DiscordPosterService' },
  { id: 'kafka-producer-service', label: 'KafkaProducerService' },
  { id: 'kafka-consumer-service', label: 'KafkaConsumerService' },
  { id: 'kafka-metrics-publisher', label: 'KafkaMetricsPublisher' },
  { id: 'meme-event-publisher', label: 'MemeEventPublisher' },
  { id: 'sse-emitter-service', label: 'SseEmitterService' },
]

const activeTab = ref('meme-pipeline')
const expandedSections = ref(new Set(['meme-pipeline-overview']))

function switchTab(tabId: string) {
  activeTab.value = tabId
  expandedSections.value = new Set([`${tabId}-overview`])
}

function toggleSection(sectionId: string) {
  const next = new Set(expandedSections.value)
  if (next.has(sectionId)) {
    next.delete(sectionId)
  } else {
    next.add(sectionId)
  }
  expandedSections.value = next
}
</script>

<style scoped>
.services-docs {
  display: grid;
  grid-template-columns: 7fr 3fr;
  gap: 24px;
  padding: 24px 0;
  height: calc(100vh - 56px - 48px);
  grid-template-rows: 1fr;
}

.docs-content {
  min-width: 0;
  overflow-y: auto;
  scrollbar-width: none;
}

.docs-content::-webkit-scrollbar {
  display: none;
}

.feed-sidebar {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.page-title {
  font-size: 1.6rem;
  font-weight: 700;
  color: #e0e0e0;
  margin: 0 0 20px;
}

/* Scrollable Tab Bar */
.tab-bar {
  display: flex;
  overflow-x: auto;
  white-space: nowrap;
  background-color: #1a1a2e;
  border: 1px solid #2a2a4a;
  border-radius: 8px 8px 0 0;
  padding: 0;
  margin-bottom: 16px;
  scrollbar-width: thin;
  scrollbar-color: #2a2a4a #1a1a2e;
}

.tab-bar::-webkit-scrollbar {
  height: 4px;
}

.tab-bar::-webkit-scrollbar-track {
  background: #1a1a2e;
}

.tab-bar::-webkit-scrollbar-thumb {
  background-color: #2a2a4a;
  border-radius: 2px;
}

.tab-button {
  padding: 12px 20px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: #a0a0b8;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
  flex-shrink: 0;
}

.tab-button:hover {
  color: #e0e0e0;
}

.tab-button.active {
  color: #e0e0e0;
  border-bottom-color: #bb86fc;
}

/* Prose styling inside DocsSection slots */
:deep(p) {
  margin: 0 0 12px;
  color: #c0c0d8;
  line-height: 1.7;
}

:deep(h4) {
  font-size: 1rem;
  font-weight: 600;
  color: #bb86fc;
  margin: 16px 0 8px;
}

:deep(ul) {
  margin: 0 0 12px;
  padding-left: 20px;
}

:deep(li) {
  margin-bottom: 6px;
  color: #c0c0d8;
  line-height: 1.6;
}

:deep(code) {
  background-color: #2a2a4a;
  color: #bb86fc;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.88em;
}

:deep(strong) {
  color: #e0e0e0;
  font-weight: 600;
}
</style>
