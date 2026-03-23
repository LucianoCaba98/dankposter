<template>
  <div class="communication-docs">
    <div class="docs-content">
      <h2 class="page-title">Communication</h2>

      <!-- Kafka Implementation -->
      <DocsSection
        title="Kafka Implementation"
        section-id="kafka"
        :expanded="expandedSections.has('kafka')"
        @toggle="toggleSection"
      >
        <h4>What is Kafka?</h4>
        <p>
          <strong>Apache Kafka</strong> is a distributed event streaming platform designed for
          high-throughput, fault-tolerant messaging. DankPoster uses Kafka as the backbone of
          its meme delivery pipeline, decoupling the ingestion phase from the Discord posting phase.
        </p>
        <p>Core concepts:</p>
        <ul>
          <li><strong>Topics</strong> — named channels where messages are published. DankPoster uses a single topic: <code>meme-delivery</code></li>
          <li><strong>Partitions</strong> — topics are split into partitions for parallelism. Messages with the same key always land on the same partition</li>
          <li><strong>Consumer Groups</strong> — a group of consumers that coordinate to read from a topic. DankPoster uses the group <code>dankposter-delivery</code></li>
          <li><strong>Offsets</strong> — each message in a partition has a unique offset. Consumers track offsets to know which messages have been processed</li>
        </ul>

        <h4>DankPoster Kafka Architecture</h4>
        <p>
          Kafka is a <strong>permanent, always-on</strong> subsystem in DankPoster. Every meme
          flows through Kafka on its way to Discord — there is no alternative delivery path
          and no feature toggle.
        </p>
        <p>The delivery flow:</p>
        <ul>
          <li><strong>DB Save</strong> — meme is persisted with status <code>FETCHED</code></li>
          <li><strong>MemeDeliveryEvent</strong> — a record containing <code>memeId</code>, <code>title</code>, <code>imageUrl</code>, and <code>danknessScore</code> is created</li>
          <li><strong>Kafka Topic</strong> — the event is published to the <code>meme-delivery</code> topic, keyed by <code>memeId</code></li>
          <li><strong>KafkaConsumerService</strong> — picks up the event, looks up the meme, and posts it to Discord</li>
          <li><strong>Discord</strong> — the meme is delivered as an embed message via the Bot API</li>
        </ul>

        <h4>Pipeline Timing</h4>
        <p>
          The pipeline fetches memes every <strong>30 minutes</strong> from all enabled sources
          (Reddit and Giphy). After fetching and deduplication, the batch is intercalated by source
          using a round-robin strategy — alternating between GIPHY and REDDIT memes for variety.
          Each meme is then delivered to Kafka with a <strong>20-second</strong> delay between publishes,
          ensuring steady, paced delivery to Discord.
        </p>

        <h4>Intercalated Delivery Ordering</h4>
        <p>
          Before delivery, the batch of fetched memes is passed through
          <code>MemeIntercalator.intercalate()</code>, which applies round-robin ordering by source.
          The algorithm partitions memes into source groups (GIPHY, REDDIT), then alternates
          between them — one GIPHY, one REDDIT, one GIPHY, and so on. When one source is
          exhausted, the remaining memes from the larger group are appended in their original
          fetch order. This ensures the Discord channel receives a varied mix of content.
        </p>

        <h4>KafkaAutoConfiguration</h4>
        <p>
          The <code>KafkaAutoConfiguration</code> class is a Spring <code>@Configuration</code>
          that creates all Kafka infrastructure beans. It loads unconditionally — Kafka is always on.
        </p>
        <p>Beans created:</p>
        <ul>
          <li><strong>ProducerFactory</strong> — configures bootstrap servers, string serializers, retry policy (3 retries, 1s backoff)</li>
          <li><strong>ConsumerFactory</strong> — configures bootstrap servers, consumer group, string deserializers, manual commit</li>
          <li><strong>KafkaTemplate</strong> — Spring Kafka's producer abstraction, wraps the ProducerFactory</li>
          <li><strong>ConcurrentKafkaListenerContainerFactory</strong> — creates listener containers with manual acknowledgment mode</li>
        </ul>
        <pre class="code-block"><code>@Configuration
@RequiredArgsConstructor
public class KafkaAutoConfiguration {

    private final KafkaProperties kafkaProperties;

    @Bean
    public ProducerFactory&lt;String, String&gt; kafkaProducerFactory() {
        Map&lt;String, Object&gt; props = new HashMap&lt;&gt;();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                  kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                  StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                  StringSerializer.class);
        return new DefaultKafkaProducerFactory&lt;&gt;(props);
    }

    @Bean
    public KafkaTemplate&lt;String, String&gt; kafkaTemplate(
            ProducerFactory&lt;String, String&gt; factory) {
        return new KafkaTemplate&lt;&gt;(factory);
    }
}</code></pre>

        <h4>KafkaProperties</h4>
        <p>
          <code>KafkaProperties</code> binds configuration from the <code>meme.stream.kafka.*</code>
          namespace. It provides three properties:
        </p>
        <ul>
          <li><code>bootstrapServers</code> — Kafka broker addresses (default: <code>localhost:9092</code>)</li>
          <li><code>topic</code> — the delivery topic name (default: <code>meme-delivery</code>)</li>
          <li><code>consumerGroup</code> — the consumer group ID (default: <code>dankposter-delivery</code>)</li>
        </ul>
        <pre class="code-block"><code>@Data
@Component
@ConfigurationProperties(prefix = "meme.stream.kafka")
public class KafkaProperties {
    private String bootstrapServers = "localhost:9092";
    private String topic = "meme-delivery";
    private String consumerGroup = "dankposter-delivery";
}</code></pre>

        <h4>KafkaProducerService</h4>
        <p>
          <code>KafkaProducerService</code> serializes <code>MemeDeliveryEvent</code> records
          to JSON and publishes them to the Kafka topic via <code>KafkaTemplate</code>.
          Messages are keyed by <code>memeId</code> to ensure partition-level ordering for the same meme.
        </p>
        <pre class="code-block"><code>public void publishDeliveryEvent(MemeDeliveryEvent event) {
    String topic = kafkaProperties.getTopic();
    String key = String.valueOf(event.memeId());
    String value = objectMapper.writeValueAsString(event);
    kafkaTemplate.send(topic, key, value)
        .whenComplete((result, ex) -&gt; {
            if (ex != null) {
                log.error("Failed to publish: {}", ex.getMessage());
            } else {
                kafkaMetricsPublisher.publishProduced(topic, key, value);
            }
        });
}</code></pre>

        <h4>KafkaConsumerService</h4>
        <p>
          <code>KafkaConsumerService</code> listens on the <code>meme-delivery</code> topic
          using <code>@KafkaListener</code> with manual acknowledgment. It implements idempotent
          delivery by checking the meme's status before posting — if a meme is already
          <code>POSTED</code>, it is skipped.
        </p>
        <p>Processing flow:</p>
        <ul>
          <li>Deserialize the <code>ConsumerRecord</code> into a <code>MemeDeliveryEvent</code></li>
          <li>Publish a "consumed" metric via <code>KafkaMetricsPublisher</code></li>
          <li>Look up the meme by ID — skip if not found or already posted</li>
          <li>Post to Discord via <code>DiscordPosterService</code></li>
          <li>Update meme status to <code>POSTED</code> and save</li>
          <li>Publish a "delivered" metric (or "failed" on error)</li>
          <li>Acknowledge the message</li>
        </ul>

        <h4>MemeDeliveryEvent</h4>
        <p>
          The Kafka message payload is a Java record with four fields:
        </p>
        <pre class="code-block"><code>public record MemeDeliveryEvent(
    Long memeId,
    String title,
    String imageUrl,
    Double danknessScore
) {}</code></pre>
      </DocsSection>

      <!-- SSE Implementation -->
      <DocsSection
        title="SSE (Server-Sent Events) Implementation"
        section-id="sse"
        :expanded="expandedSections.has('sse')"
        @toggle="toggleSection"
      >
        <h4>SseEmitterService</h4>
        <p>
          <code>SseEmitterService</code> manages three SSE channels for real-time backend-to-frontend
          communication:
        </p>
        <ul>
          <li><code>ingestion</code> — fires when new memes are fetched and saved</li>
          <li><code>posted</code> — fires when a meme is successfully delivered to Discord</li>
          <li><code>kafka-metrics</code> — fires on Kafka lifecycle events (produced, consumed, delivered, failed)</li>
        </ul>
        <p>
          Channels are stored in a <code>ConcurrentHashMap&lt;String, CopyOnWriteArrayList&lt;SseEmitter&gt;&gt;</code>,
          providing thread-safe management of multiple concurrent clients per channel.
          Each emitter is created with a <strong>30-minute timeout</strong> (1,800,000ms).
        </p>
        <p>
          Lifecycle callbacks (<code>onCompletion</code>, <code>onTimeout</code>, <code>onError</code>)
          automatically remove dead emitters from the channel list. The <code>broadcast()</code> method
          iterates over all emitters in a channel and sends data; failed emitters are removed on
          <code>IOException</code>.
        </p>

        <h4>SseController</h4>
        <p>
          <code>SseController</code> exposes SSE endpoints at <code>/api/events/{channel}</code>.
          When a client connects, it calls <code>SseEmitterService.createEmitter(channel)</code>
          and returns the <code>SseEmitter</code> to establish the event stream.
        </p>
        <pre class="code-block"><code>@GetMapping("/api/events/{channel}")
public SseEmitter subscribe(@PathVariable String channel) {
    return sseEmitterService.createEmitter(channel);
}</code></pre>

        <h4>MemeEventPublisher</h4>
        <p>
          <code>MemeEventPublisher</code> broadcasts meme lifecycle events to the frontend:
        </p>
        <ul>
          <li><code>publishIngested(List&lt;Meme&gt;)</code> — broadcasts each newly saved meme as a <code>MemeDto</code> on the <code>ingestion</code> channel</li>
          <li><code>publishPosted(Meme)</code> — broadcasts a single meme on the <code>posted</code> channel after successful Discord delivery</li>
        </ul>
        <p>
          It converts <code>Meme</code> entities to <code>MemeDto</code> before broadcasting,
          ensuring the frontend receives a clean, serializable representation.
        </p>

        <h4>KafkaMetricsPublisher</h4>
        <p>
          <code>KafkaMetricsPublisher</code> broadcasts Kafka lifecycle events as
          <code>KafkaMessageEvent</code> records on the <code>kafka-metrics</code> SSE channel.
          It tracks four delivery stages:
        </p>
        <ul>
          <li><strong>produced</strong> — message published to Kafka topic</li>
          <li><strong>consumed</strong> — message read by the consumer</li>
          <li><strong>delivered</strong> — meme successfully posted to Discord</li>
          <li><strong>failed</strong> — Discord posting failed</li>
        </ul>
        <p>
          These events power the <strong>MetricsPanel</strong> and <strong>MessageInspector</strong>
          components in the Admin Panel's Kafka Metrics tab.
        </p>

        <h4>Frontend Composables</h4>
        <p>
          The frontend uses two composables for SSE consumption:
        </p>
        <ul>
          <li><code>useSse(url)</code> — generic composable that connects to any SSE endpoint and exposes the latest event as a reactive ref</li>
          <li><code>useKafkaMetricsSse()</code> — Kafka-specific composable that connects to <code>/api/events/kafka-metrics</code> and provides typed <code>KafkaMessageEvent</code> data with connect/disconnect lifecycle methods</li>
        </ul>
      </DocsSection>

      <!-- Discord Bot API -->
      <DocsSection
        title="Discord Bot API"
        section-id="discord"
        :expanded="expandedSections.has('discord')"
        @toggle="toggleSection"
      >
        <h4>DiscordPosterService</h4>
        <p>
          <code>DiscordPosterService</code> posts memes to a Discord channel via the Discord Bot API
          using a reactive <code>WebClient</code>. It delegates message formatting to
          <code>MemeRenderService</code>, which uses the <strong>Strategy pattern</strong> to select
          source-specific renderers.
        </p>
        <p>
          The <code>post()</code> method returns a <code>Mono&lt;Meme&gt;</code>, making it fully
          composable within the reactive pipeline.
        </p>

        <h4>MemeRenderService &amp; Strategy Pattern</h4>
        <p>
          <code>MemeRenderService</code> selects the appropriate renderer based on the meme's source:
        </p>
        <ul>
          <li><strong>GiphyMemeRenderer</strong> — formats Giphy GIFs with appropriate embed styling</li>
          <li><strong>RedditMemeRenderer</strong> — formats Reddit image posts with subreddit attribution</li>
        </ul>
        <p>
          Each renderer implements the <code>MemeRenderer</code> interface and produces a
          <code>DiscordMessagePayload</code> containing embed objects ready for the Discord API.
        </p>

        <h4>Discord API Integration</h4>
        <p>
          Messages are posted to <code>/channels/{channelId}/messages</code> with embed payloads.
          The bot token and channel ID are injected from environment variables via <code>DiscordConfig</code>:
        </p>
        <ul>
          <li><code>DISCORD_BOT_TOKEN</code> — the bot's authentication token</li>
          <li><code>DISCORD_CHANNEL_ID</code> — the target channel for meme delivery</li>
        </ul>
        <pre class="code-block"><code>public Mono&lt;Meme&gt; post(Meme meme) {
    DiscordMessagePayload payload = memeRenderService.render(meme);
    return discordClient.post()
        .uri("/channels/{channelId}/messages",
             discordConfig.getChannelId())
        .bodyValue(payload)
        .retrieve()
        .bodyToMono(String.class)
        .thenReturn(meme);
}</code></pre>
      </DocsSection>

      <!-- Communication Layer Connections -->
      <DocsSection
        title="Communication Layer Connections"
        section-id="connections"
        :expanded="expandedSections.has('connections')"
        @toggle="toggleSection"
      >
        <h4>Three Communication Layers</h4>
        <p>
          DankPoster uses three distinct communication mechanisms, each serving a different purpose:
        </p>
        <ul>
          <li><strong>Kafka</strong> — asynchronous event streaming for meme delivery. Decouples the ingestion pipeline from Discord posting, providing fault tolerance and replay capability</li>
          <li><strong>SSE (Server-Sent Events)</strong> — real-time push from backend to frontend. Powers live feeds, ingestion notifications, and Kafka metrics dashboards</li>
          <li><strong>Discord Bot API</strong> — external delivery to Discord channels. The final destination for every meme in the pipeline</li>
        </ul>

        <h4>End-to-End Data Flow</h4>
        <p>
          Here is how the three layers connect in the full delivery lifecycle:
        </p>
        <ul>
          <li><strong>Pipeline fetches memes</strong> → saves to DB → broadcasts <code>ingestion</code> SSE events to the frontend</li>
          <li><strong>Pipeline publishes to Kafka</strong> → <code>MemeDeliveryEvent</code> lands on the <code>meme-delivery</code> topic → <code>kafka-metrics</code> SSE "produced" event fires</li>
          <li><strong>Consumer reads from Kafka</strong> → <code>kafka-metrics</code> SSE "consumed" event fires</li>
          <li><strong>Consumer posts to Discord</strong> → meme appears in the Discord channel → meme status updated to <code>POSTED</code></li>
          <li><strong>Consumer broadcasts results</strong> → <code>posted</code> SSE event fires (Live Meme Feed updates) → <code>kafka-metrics</code> SSE "delivered" event fires (Metrics Panel updates)</li>
        </ul>

        <h4>Frontend SSE Channels</h4>
        <p>
          The frontend receives SSE events on three channels, each powering different UI components:
        </p>
        <ul>
          <li><code>ingestion</code> — new memes fetched and saved. Powers the <strong>Ingestion Feed</strong> and triggers <strong>Posting Queue</strong> refresh</li>
          <li><code>posted</code> — memes delivered to Discord. Powers the <strong>Live Meme Feed</strong> sidebar and triggers Posting Queue removal</li>
          <li><code>kafka-metrics</code> — Kafka lifecycle events. Powers the <strong>Metrics Panel</strong> and <strong>Message Inspector</strong> in the Admin Panel</li>
        </ul>
      </DocsSection>
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

const expandedSections = ref(new Set(['kafka']))

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
.communication-docs {
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

/* Code blocks */
:deep(.code-block) {
  background-color: #0f0f1a;
  border: 1px solid #2a2a4a;
  border-radius: 8px;
  padding: 16px;
  overflow-x: auto;
  margin: 12px 0;
  font-size: 0.85rem;
  line-height: 1.6;
}

:deep(.code-block code) {
  background: none;
  padding: 0;
  color: #c0c0d8;
  font-family: 'Fira Code', 'Cascadia Code', 'Consolas', monospace;
  white-space: pre;
}
</style>
