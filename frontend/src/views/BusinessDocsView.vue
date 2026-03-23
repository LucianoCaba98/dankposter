<template>
  <div class="business-docs">
    <div class="docs-content">
      <h2 class="page-title">Business Model &amp; Design</h2>

      <DocsSection
        title="Application Purpose"
        section-id="purpose"
        :expanded="expandedSections.has('purpose')"
        @toggle="toggleSection"
      >
        <p>
          <strong>DankPoster</strong> is a reactive meme ingestion and distribution pipeline.
          It continuously fetches memes from <strong>Reddit</strong> and <strong>Giphy</strong>,
          persists them with deduplication, and posts them to a <strong>Discord</strong> channel
          on a timed interval.
        </p>
        <p>
          The project was built as a learning platform for modern backend patterns:
        </p>
        <ul>
          <li><strong>Spring Boot reactive programming</strong> with Project Reactor and WebFlux</li>
          <li><strong>Kafka event streaming</strong> as a permanent delivery backbone</li>
          <li><strong>SQS integration</strong> for alternative message queue consumption</li>
          <li><strong>Pluggable source architecture</strong> via the <code>MemeSource</code> interface with <code>@ConditionalOnProperty</code></li>
        </ul>
        <p>
          Every 30 minutes the pipeline fetches a batch of memes from all enabled sources,
          deduplicates them against the database, intercalates them by source for variety,
          and delivers them one-by-one through Kafka to Discord every 20 seconds.
        </p>
      </DocsSection>

      <DocsSection
        title="Architecture Overview"
        section-id="architecture"
        :expanded="expandedSections.has('architecture')"
        @toggle="toggleSection"
      >
        <p>
          DankPoster follows a <strong>Source → Persistence → Delivery</strong> pipeline architecture.
          Each stage is decoupled and independently configurable.
        </p>
        <h4>Sources</h4>
        <p>
          Meme sources are pluggable via the <code>MemeSource</code> interface. Each implementation
          is conditionally loaded with <code>@ConditionalOnProperty</code>, making it easy to
          enable or disable individual sources without code changes. Currently supported:
        </p>
        <ul>
          <li><strong>RedditMemeSource</strong> — fetches top posts from configured subreddits via Reddit's JSON API</li>
          <li><strong>GiphyMemeSource</strong> — fetches trending GIFs from the Giphy API</li>
        </ul>
        <h4>Persistence</h4>
        <p>
          Memes are persisted using <strong>Spring Data JPA</strong> with <strong>H2</strong> in development
          and <strong>PostgreSQL</strong> in production. Deduplication is handled by checking the
          <code>externalId</code> field before saving, ensuring the same meme is never stored twice.
        </p>
        <h4>Delivery</h4>
        <p>
          Delivery always routes through <strong>Apache Kafka</strong>. The producer publishes
          <code>MemeDeliveryEvent</code> messages to the <code>meme-delivery</code> topic.
          The consumer picks them up and posts to Discord via the Bot API.
          Kafka is a permanent, always-on subsystem — no feature toggle required.
        </p>

        <div class="flow-diagram">
          <div class="flow-row">
            <div class="flow-box source">Reddit</div>
            <div class="flow-box source">Giphy</div>
          </div>
          <div class="flow-arrow">▼</div>
          <div class="flow-row">
            <div class="flow-box pipeline">MemeFetchService</div>
          </div>
          <div class="flow-arrow">▼</div>
          <div class="flow-row">
            <div class="flow-box persistence">DB Save + Dedup</div>
          </div>
          <div class="flow-arrow">▼</div>
          <div class="flow-row">
            <div class="flow-box pipeline">MemeIntercalator</div>
          </div>
          <div class="flow-arrow">▼</div>
          <div class="flow-row">
            <div class="flow-box kafka">Kafka Topic</div>
          </div>
          <div class="flow-arrow">▼</div>
          <div class="flow-row">
            <div class="flow-box delivery">Discord Bot API</div>
          </div>
        </div>
      </DocsSection>

      <DocsSection
        title="Data Flow"
        section-id="dataflow"
        :expanded="expandedSections.has('dataflow')"
        @toggle="toggleSection"
      >
        <p>
          The data flow is orchestrated by <strong>MemePipeline</strong> and spans four stages:
        </p>
        <h4>1. Fetch (every 30 minutes)</h4>
        <p>
          <code>MemeFetchService</code> aggregates all registered <code>MemeSource</code> implementations
          and calls each one concurrently. Results are merged into a single reactive stream.
        </p>
        <h4>2. Persist with Deduplication</h4>
        <p>
          Each fetched meme is checked against the database by <code>externalId</code>.
          New memes are saved with status <code>FETCHED</code>; duplicates are silently skipped.
        </p>
        <h4>3. Intercalate by Source</h4>
        <p>
          The batch of newly saved memes is passed through <code>MemeIntercalator.intercalate()</code>,
          which applies a round-robin ordering — alternating between GIPHY and REDDIT sources.
          This ensures the Discord channel receives a varied mix of content rather than
          all memes from one source followed by all from another.
        </p>
        <h4>4. Publish to Kafka (every 20 seconds)</h4>
        <p>
          The intercalated list is delivered one meme at a time with a 20-second delay between each.
          <code>KafkaProducerService</code> publishes a <code>MemeDeliveryEvent</code> to the
          <code>meme-delivery</code> topic. <code>KafkaConsumerService</code> picks up each event
          and posts the meme to Discord via <code>DiscordPosterService</code>.
        </p>
        <h4>SSE Broadcasting</h4>
        <p>
          Throughout the pipeline, Server-Sent Events are broadcast to the frontend:
        </p>
        <ul>
          <li><strong>Ingestion events</strong> — fired when new memes are fetched and saved</li>
          <li><strong>Posted events</strong> — fired when a meme is successfully delivered to Discord</li>
          <li><strong>Kafka metrics events</strong> — fired on produce, consume, deliver, and fail lifecycle stages</li>
        </ul>
      </DocsSection>

      <DocsSection
        title="Technology Stack"
        section-id="techstack"
        :expanded="expandedSections.has('techstack')"
        @toggle="toggleSection"
      >
        <h4>Backend</h4>
        <ul>
          <li><strong>Java 17</strong> — records, pattern matching, text blocks</li>
          <li><strong>Spring Boot 3.5</strong> — auto-configuration, dependency injection, scheduling</li>
          <li><strong>Spring WebFlux</strong> — reactive <code>WebClient</code> for all external HTTP calls</li>
          <li><strong>Spring Data JPA</strong> — repository abstraction over H2 (dev) / PostgreSQL (prod)</li>
          <li><strong>Apache Kafka</strong> — event streaming for meme delivery pipeline</li>
          <li><strong>Project Reactor</strong> — <code>Mono</code> and <code>Flux</code> for reactive composition</li>
          <li><strong>Lombok</strong> — boilerplate reduction with <code>@Data</code>, <code>@Builder</code>, <code>@Slf4j</code></li>
        </ul>
        <h4>Frontend</h4>
        <ul>
          <li><strong>Vue 3</strong> — Composition API with <code>&lt;script setup&gt;</code></li>
          <li><strong>TypeScript</strong> — type-safe component development</li>
          <li><strong>Vite</strong> — fast dev server and optimized production builds</li>
        </ul>
        <h4>Infrastructure</h4>
        <ul>
          <li><strong>Docker</strong> — Alpine JRE 17 base image for lightweight containers</li>
          <li><strong>H2 Database</strong> — zero-config in-memory database for development</li>
          <li><strong>PostgreSQL</strong> — production-ready relational database</li>
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

const expandedSections = ref(new Set(['purpose']))

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
.business-docs {
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

/* Flow diagram */
.flow-diagram {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  margin: 20px 0 8px;
}

.flow-row {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.flow-box {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  text-align: center;
  min-width: 140px;
  border: 1px solid #2a2a4a;
}

.flow-box.source {
  background-color: #1a2a1a;
  color: #4caf50;
  border-color: #2a4a2a;
}

.flow-box.pipeline {
  background-color: #1a1a2e;
  color: #bb86fc;
  border-color: #3a2a5a;
}

.flow-box.persistence {
  background-color: #2a1a1a;
  color: #ff7043;
  border-color: #4a2a2a;
}

.flow-box.kafka {
  background-color: #1a2a2e;
  color: #26c6da;
  border-color: #2a4a4a;
}

.flow-box.delivery {
  background-color: #2a2a1a;
  color: #ffd54f;
  border-color: #4a4a2a;
}

.flow-arrow {
  color: #a0a0b8;
  font-size: 1.2rem;
  line-height: 1;
}
</style>
