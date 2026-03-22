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
- **Sequential processing:** Messages are processed one at a time. This is intentional — it prevents race conditions in deduplication. If two identical memes arrive simultaneously, sequential processing ensures only one gets saved.
- **Dead Letter Queue (DLQ):** If a message fails processing repeatedly, SQS automatically moves it to the DLQ. You configure this on the AWS side, not in code.
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

**Why use `memeId` as the Kafka key?** Kafka guarantees ordering within a partition. Messages with the same key go to the same partition. Using `memeId` as the key means all events for a single meme are ordered — useful if you ever have update/delete events later.

**Retry:** The Kafka producer is configured with 3 retries and 1-second backoff in `KafkaAutoConfiguration`. This happens at the Kafka client level, transparent to this service.

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
            └─ acknowledgment.acknowledge()  ← manual offset commit
            └─ on Discord failure: log ERROR, do NOT ack (Kafka redelivers)
```

**Key concepts:**
- **`@KafkaListener`:** Spring Kafka's annotation that turns a method into a Kafka consumer. It handles polling, deserialization, and threading for you.
- **Manual acknowledgment:** We only call `acknowledgment.acknowledge()` after Discord succeeds. If Discord is down, the message stays unacknowledged and Kafka redelivers it later. No memes lost.
- **Consumer group:** All instances of DankPoster share the same group ID. Kafka assigns partitions across instances so each event is processed by exactly one instance.

---

### Modified Existing Files

#### `service/RedditFetcherService.java` — Now SQS-aware

Two new `Optional` fields were added:
- `Optional<SqsProducerService>` — present when SQS is enabled
- `Optional<KafkaProducerService>` — present when Kafka is enabled

The `fetchMemesFromSubreddit()` method now branches:
- **SQS on:** Converts each meme to `MemeMessage` and sends to SQS. No direct DB save.
- **SQS off:** Saves directly to DB (original behavior). If Kafka is on, also publishes `MemeDeliveryEvent` for each saved meme.

#### `scheduler/MemeScheduler.java` — Kafka-aware poster

The `memePoster()` method now checks `kafkaProperties.isEnabled()` at the top. When Kafka is handling delivery, the scheduler short-circuits — no double-posting.

---

## Environment Variables (new ones)

Add these to your `.ENV` file:

```bash
# SQS Integration
SQS_ENABLED=false          # set to "true" to enable
SQS_QUEUE_URL=             # your SQS queue URL
SQS_DLQ_URL=               # your dead letter queue URL
AWS_REGION=us-east-1       # AWS region
SQS_POLL_INTERVAL=10s      # how often to poll SQS

# Kafka Integration
KAFKA_ENABLED=false                    # set to "true" to enable
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 # Kafka broker address(es)
KAFKA_TOPIC=meme-delivery             # topic name for delivery events
KAFKA_CONSUMER_GROUP=dankposter-delivery # consumer group ID
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
Memes go through SQS before being saved. Delivery still uses the scheduler.

### Mode 3: Kafka only
```bash
KAFKA_ENABLED=true
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```
Memes are saved directly (no SQS), but delivery is event-driven via Kafka instead of the scheduler.

### Mode 4: SQS + Kafka (full pipeline)
```bash
SQS_ENABLED=true
SQS_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789/dankposter-memes
SQS_DLQ_URL=https://sqs.us-east-1.amazonaws.com/123456789/dankposter-memes-dlq
KAFKA_ENABLED=true
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```
Full decoupled pipeline: fetch → SQS → persist → Kafka → Discord.

---

## SQS Concepts for Beginners

**What is SQS?** Amazon Simple Queue Service. Think of it as a mailbox — you put messages in, and someone else picks them up later. The sender and receiver don't need to be online at the same time.

**Why use it here?** If Reddit returns 50 memes at once, they all go into the queue. The consumer processes them one at a time at its own pace. If the consumer crashes, the messages stay in the queue and get processed when it comes back.

**Dead Letter Queue (DLQ):** A special queue for messages that failed processing too many times. Instead of retrying forever, SQS moves them to the DLQ so you can investigate later. You configure the max retry count on the AWS side.

**Visibility timeout:** When a consumer picks up a message, SQS hides it from other consumers for a period. If the consumer doesn't delete the message in time (e.g., it crashed), the message becomes visible again for retry.

---

## Kafka Concepts for Beginners

**What is Kafka?** A distributed event streaming platform. Think of it as a log — events are appended in order, and consumers read from wherever they left off.

**Topic:** A named stream of events. All meme delivery events go to the `meme-delivery` topic.

**Partition:** Topics are split into partitions for parallelism. Messages with the same key (our `memeId`) always go to the same partition, preserving order per meme.

**Consumer group:** A group of consumers that share the work. Kafka assigns partitions to consumers in the group. If you have 3 partitions and 3 consumers, each gets one partition. If a consumer dies, its partitions are reassigned.

**Offset:** A number that tracks where a consumer is in a partition. After processing a message, the consumer "commits" the offset. On restart, it picks up from the last committed offset.

**Why manual offset commit?** By default, Kafka auto-commits periodically. But if the consumer crashes between auto-commit and actually finishing the work, the message is lost. Manual commit means we only say "done" after Discord confirms the post went through.

**Retries:** The Kafka producer is configured with 3 retries and 1-second backoff. If the broker is temporarily unavailable, the client retries automatically before giving up.

---

## File Map

```
src/main/java/com/dankposter/
├── config/
│   ├── SqsProperties.java          ← NEW: SQS config binding
│   ├── KafkaProperties.java        ← NEW: Kafka config binding
│   ├── SqsAutoConfiguration.java   ← NEW: SQS bean wiring (conditional)
│   └── KafkaAutoConfiguration.java ← NEW: Kafka bean wiring (conditional)
├── dto/
│   ├── MemeMessage.java            ← NEW: SQS message payload
│   └── MemeDeliveryEvent.java      ← NEW: Kafka event payload
├── service/
│   ├── SqsProducerService.java     ← NEW: sends memes to SQS
│   ├── SqsConsumerService.java     ← NEW: reads memes from SQS
│   ├── KafkaProducerService.java   ← NEW: publishes delivery events
│   └── KafkaConsumerService.java   ← NEW: consumes events, posts to Discord
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
