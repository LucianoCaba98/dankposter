# SQS & Kafka Integration — Playbook

A hands-on guide to every new file in the SQS-Kafka integration. If you're new to SQS and Kafka, start here.

---

## The Big Picture

Before this change, DankPoster was a straight line:

```
Reddit → save to DB → scheduler picks top meme → post to Discord
```

Now there are two optional message layers you can toggle on independently:

```
Reddit → [SQS queue] → save to DB → [Kafka topic] → post to Discord
```

- **SQS** decouples *fetching* from *persisting*. Memes go into a queue first, then a consumer pulls them out, deduplicates, and saves.
- **Kafka** decouples *persisting* from *delivering*. After a meme is saved, an event fires on a Kafka topic, and a consumer picks it up to post to Discord.

Both are off by default. The existing flow works exactly as before unless you flip the toggles.

---

## Feature Toggles (the most important part)

| Toggle | Env Var | Default | What it does |
|---|---|---|---|
| `meme.queue.sqs.enabled` | `SQS_ENABLED` | `false` | Activates SQS producer + consumer beans |
| `meme.stream.kafka.enabled` | `KAFKA_ENABLED` | `false` | Activates Kafka producer + consumer beans |

### Toggle Combinations

| SQS | Kafka | Ingestion | Delivery |
|---|---|---|---|
| off | off | Fetcher saves directly to DB (original) | Scheduler posts to Discord (original) |
| on | off | Fetcher → SQS → Consumer → DB | Scheduler posts to Discord (original) |
| off | on | Fetcher saves directly to DB (original) | DB save → Kafka event → Consumer → Discord |
| on | on | Fetcher → SQS → Consumer → DB | DB save → Kafka event → Consumer → Discord |

This works because every new bean uses `@ConditionalOnProperty` — Spring won't even instantiate them when disabled.

---

## New Files — What Each One Does

### DTOs (Data Transfer Objects)

#### `dto/MemeMessage.java` — SQS payload

A Java record that represents a meme traveling through SQS. Think of it as the "envelope" for a meme between fetching and saving.

```java
public record MemeMessage(
    String title,            // meme title
    String imageUrl,         // the image link
    String sourceIdentifier, // "reddit", "giphy", etc.
    String sourceSpecificId, // Reddit post ID, Giphy GIF ID, etc.
    Double danknessScore     // pre-computed quality score
) {}
```

**Why a record?** Records are immutable, concise, and Jackson serializes them out of the box. Perfect for message payloads.

**Where it flows:** `RedditFetcherService` → serialized to JSON → SQS queue → deserialized by `SqsConsumerService`

---

#### `dto/MemeDeliveryEvent.java` — Kafka payload

Same idea, but for the Kafka side. This is the event that says "hey, this meme is saved and ready to post."

```java
public record MemeDeliveryEvent(
    Long memeId,         // database primary key (assigned after save)
    String title,        // meme title
    String imageUrl,     // the image link
    Double danknessScore // quality score
) {}
```

**Where it flows:** `SqsConsumerService` (after DB save) → serialized to JSON → Kafka topic → deserialized by `KafkaConsumerService`

---

### Configuration

#### `config/SqsProperties.java` — SQS settings

Binds to `meme.queue.sqs.*` in `application.yml`. All values come from environment variables.

```java
@ConfigurationProperties(prefix = "meme.queue.sqs")
public class SqsProperties {
    private boolean enabled = false;           // master toggle
    private String queueUrl;                   // SQS_QUEUE_URL
    private String dlqUrl;                     // SQS_DLQ_URL
    private String region = "us-east-1";       // AWS_REGION
    private Duration pollInterval = Duration.ofSeconds(10); // SQS_POLL_INTERVAL
}
```

**Key concept — `pollInterval`:** This controls how often the consumer checks SQS for new messages. 10 seconds is a safe default. Lower = faster processing, higher = less AWS cost.

---

#### `config/KafkaProperties.java` — Kafka settings

Binds to `meme.stream.kafka.*` in `application.yml`.

```java
@ConfigurationProperties(prefix = "meme.stream.kafka")
public class KafkaProperties {
    private boolean enabled = false;                        // master toggle
    private String bootstrapServers = "localhost:9092";     // KAFKA_BOOTSTRAP_SERVERS
    private String topic = "meme-delivery";                 // KAFKA_TOPIC
    private String consumerGroup = "dankposter-delivery";   // KAFKA_CONSUMER_GROUP
}
```

**Key concepts:**
- **Bootstrap servers:** The address(es) of your Kafka cluster. In dev, it's usually `localhost:9092`.
- **Topic:** A named channel in Kafka. All meme delivery events go to this one topic.
- **Consumer group:** Kafka uses this to track which messages a consumer has already read. If you scale to multiple instances, they share the same group so each message is processed once.

---

#### `config/SqsAutoConfiguration.java` — SQS bean wiring

Only loaded when `meme.queue.sqs.enabled=true`. Creates:
- `SqsAsyncClient` — the AWS SDK client that talks to SQS, configured with the region from `SqsProperties`
- `SqsTemplate` — Spring's higher-level abstraction for sending/receiving SQS messages

**What's `@ConditionalOnProperty`?** It tells Spring "only create these beans if this config value is set." When SQS is disabled, these beans don't exist at all — zero overhead.

---

#### `config/KafkaAutoConfiguration.java` — Kafka bean wiring

Only loaded when `meme.stream.kafka.enabled=true`. Creates:
- `ProducerFactory` — configures how messages are serialized and sent (String keys + String values, 3 retries with 1s backoff)
- `ConsumerFactory` — configures how messages are read (manual offset commit, String deserializers)
- `KafkaTemplate` — Spring's abstraction for sending Kafka messages
- `KafkaListenerContainerFactory` — powers the `@KafkaListener` annotation with manual ack mode

**Why manual offset commit?** By default, Kafka auto-commits "I read this message" periodically. With manual commit, we only acknowledge after successfully posting to Discord. If Discord fails, the message gets redelivered. This prevents lost memes.

---

### Services

#### `service/SqsProducerService.java` — Sends memes to SQS

The "on-ramp" to SQS. When SQS is enabled, `RedditFetcherService` calls this instead of saving directly to the DB.

```
RedditFetcherService.fetchMemesFromSubreddit()
  └─ for each scored meme:
       └─ sqsProducerService.send(memeMessage)
            └─ serialize to JSON → sqsTemplate.send(queueUrl, json)
```

**Error handling:** If a send fails, it logs the error and moves on to the next meme. One bad meme doesn't kill the whole fetch cycle.

---

#### `service/SqsConsumerService.java` — Reads memes from SQS

The "off-ramp" from SQS. Polls the queue on a timer, processes one message at a time.

```
@Scheduled(fixedDelay from poll-interval)
pollMessages()
  └─ sqsTemplate.receive(queueUrl)
       └─ deserialize JSON → MemeMessage
       └─ check MemeRepository.existsByRedditId(sourceSpecificId)
            ├─ duplicate? → delete from queue, log at DEBUG
            └─ new? → save to DB → delete from queue
                      └─ if Kafka enabled: publish MemeDeliveryEvent
```

**Key concepts:**
- **Sequential processing:** Messages are processed one at a time. This is intentional — it prevents race conditions in deduplication.
- **Dead Letter Queue (DLQ):** If a message fails processing repeatedly, SQS automatically moves it to the DLQ.
- **`Optional<KafkaProducerService>`:** Since Kafka might be disabled, this bean might not exist. `Optional` handles that gracefully.

---

#### `service/KafkaProducerService.java` — Publishes delivery events to Kafka

After a meme is saved to the DB, this service fires an event saying "new meme ready for delivery."

```
kafkaProducerService.publishDeliveryEvent(event)
  └─ serialize MemeDeliveryEvent to JSON
  └─ kafkaTemplate.send(topic, key=memeId, value=json)
       └─ on success: log at DEBUG
       └─ on failure: log at ERROR (retries already exhausted by Kafka client)
```

**Why use `memeId` as the Kafka key?** Kafka guarantees ordering within a partition. Messages with the same key go to the same partition.

**Retry:** The Kafka producer is configured with 3 retries and 1-second backoff in `KafkaAutoConfiguration`.

---

#### `service/KafkaConsumerService.java` — Posts memes to Discord via Kafka

Listens to the Kafka topic and posts memes to Discord when events arrive.

```
@KafkaListener(topic, consumerGroup)
onMessage(record, acknowledgment)
  └─ deserialize JSON → MemeDeliveryEvent
  └─ memeRepository.findById(memeId)
       ├─ not found? → log WARN, skip, ack
       ├─ already posted? → skip, ack
       └─ found + not posted:
            └─ discordPosterService.postNextUnpostedMeme()
            └─ mark meme as posted
            └─ acknowledgment.acknowledge()
            └─ on Discord failure: log ERROR, do NOT ack (Kafka redelivers)
```

**Key concepts:**
- **`@KafkaListener`:** Spring Kafka's annotation that turns a method into a Kafka consumer.
- **Manual acknowledgment:** We only call `acknowledgment.acknowledge()` after Discord succeeds.
- **Consumer group:** All instances of DankPoster share the same group ID.

---

### Modified Existing Files

#### `service/RedditFetcherService.java` — Now SQS-aware

Two new `Optional` fields were added:
- `Optional<SqsProducerService>` — present when SQS is enabled
- `Optional<KafkaProducerService>` — present when Kafka is enabled

#### `scheduler/MemeScheduler.java` — Kafka-aware poster

The `memePoster()` method now checks `kafkaProperties.isEnabled()` at the top. When Kafka is handling delivery, the scheduler short-circuits.

---

## Environment Variables (new ones)

Add these to your `.ENV` file:

```bash
# SQS Integration
SQS_ENABLED=false
SQS_QUEUE_URL=
SQS_DLQ_URL=
AWS_REGION=us-east-1
SQS_POLL_INTERVAL=10s

# Kafka Integration
KAFKA_ENABLED=false
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC=meme-delivery
KAFKA_CONSUMER_GROUP=dankposter-delivery
```

---

## How to Run Each Mode

### Mode 1: Original (no SQS, no Kafka)
Just don't set `SQS_ENABLED` or `KAFKA_ENABLED`. Everything works as before.

### Mode 2: SQS only
```bash
SQS_ENABLED=true
SQS_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789/dankposter-memes
SQS_DLQ_URL=https://sqs.us-east-1.amazonaws.com/123456789/dankposter-memes-dlq
```

### Mode 3: Kafka only
```bash
KAFKA_ENABLED=true
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### Mode 4: SQS + Kafka (full pipeline)
```bash
SQS_ENABLED=true
SQS_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789/dankposter-memes
SQS_DLQ_URL=https://sqs.us-east-1.amazonaws.com/123456789/dankposter-memes-dlq
KAFKA_ENABLED=true
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

---

## Error Handling Summary

| Scenario | What happens | Log level |
|---|---|---|
| SQS send fails | Log + skip that meme, continue batch | ERROR |
| SQS message can't be deserialized | Leave on queue for retry → DLQ | ERROR |
| Duplicate meme in SQS | Delete from queue, move on | DEBUG |
| Kafka publish fails | 3 retries with backoff, then log | ERROR |
| Kafka event for missing meme | Skip + commit offset | WARN |
| Discord posting fails (Kafka) | Don't commit offset → Kafka redelivers | ERROR |
| Meme already posted (Kafka) | Skip + commit offset | DEBUG |
