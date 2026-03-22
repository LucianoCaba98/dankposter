# Requirements Document

## Introduction

DankPoster currently operates as a self-contained pipeline: a scheduler fetches memes from Reddit, persists them to the database with deduplication, and another scheduler posts the highest-scored unposted meme to Discord. This feature introduces AWS SQS and Apache Kafka into the pipeline to decouple ingestion from delivery, enable asynchronous processing, and provide a scalable event-driven backbone. SQS acts as the ingestion queue (memes fetched from sources are enqueued), and Kafka acts as the distribution event stream (memes ready for delivery are published to topics that consumers like the Discord poster subscribe to).

## Glossary

- **Meme_Pipeline**: The end-to-end system that fetches, scores, persists, and delivers memes. Encompasses all services in the DankPoster application.
- **SQS_Producer**: The component that sends meme messages to an AWS SQS queue after fetching them from external sources (Reddit, Giphy).
- **SQS_Consumer**: The component that receives meme messages from the AWS SQS queue, applies scoring and deduplication, and persists valid memes to the database.
- **Kafka_Producer**: The component that publishes meme events to a Kafka topic after a meme has been persisted and is ready for delivery.
- **Kafka_Consumer**: The component that subscribes to a Kafka topic and triggers delivery actions (e.g., posting to Discord) when meme events are received.
- **Meme_Message**: A serialized representation of a meme (JSON) containing at minimum the meme title, image URL, source identifier, and source-specific ID.
- **SQS_Queue**: An AWS Simple Queue Service queue used as a buffer between meme fetching and meme processing/persistence.
- **Kafka_Topic**: An Apache Kafka topic used to stream meme delivery events from persistence to delivery consumers.
- **Dead_Letter_Queue**: An SQS queue that receives messages that failed processing after the configured maximum number of retry attempts.
- **Consumer_Group**: A Kafka consumer group identifier that ensures each meme delivery event is processed by exactly one consumer instance within the group.

## Requirements

### Requirement 1: SQS Producer — Enqueue Fetched Memes

**User Story:** As a pipeline operator, I want fetched memes to be sent to an SQS queue instead of being persisted directly, so that ingestion is decoupled from processing and the system can handle bursts of fetched memes without blocking.

#### Acceptance Criteria

1. WHEN the RedditFetcherService fetches memes from a subreddit, THE SQS_Producer SHALL serialize each meme as a Meme_Message in JSON format and send the message to the configured SQS_Queue.
2. THE SQS_Producer SHALL include the meme title, image URL, source identifier, source-specific ID, and dankness score in every Meme_Message.
3. IF the SQS_Producer fails to send a message to the SQS_Queue, THEN THE SQS_Producer SHALL log the error with the meme source-specific ID and source name, and continue processing remaining memes without halting the fetch cycle.
4. THE SQS_Producer SHALL be enabled or disabled via a `meme.queue.sqs.enabled` configuration property, defaulting to false.
5. WHILE the SQS_Producer is disabled, THE Meme_Pipeline SHALL continue to use the existing direct-persistence flow without modification.

### Requirement 2: SQS Consumer — Process Queued Memes

**User Story:** As a pipeline operator, I want an SQS consumer that reads meme messages from the queue, applies deduplication, and persists valid memes, so that processing happens asynchronously from fetching.

#### Acceptance Criteria

1. WHEN a Meme_Message arrives on the SQS_Queue, THE SQS_Consumer SHALL deserialize the message from JSON into a Meme entity.
2. WHEN the SQS_Consumer receives a Meme_Message, THE SQS_Consumer SHALL check for duplicates using the source-specific ID before persisting the meme to the database.
3. WHEN a duplicate meme is detected, THE SQS_Consumer SHALL acknowledge the SQS message (delete it from the queue) and log the duplicate at debug level.
4. WHEN a meme is successfully persisted, THE SQS_Consumer SHALL delete the message from the SQS_Queue.
5. IF the SQS_Consumer fails to process a Meme_Message after the configured maximum retry attempts, THEN THE SQS_Consumer SHALL allow SQS to route the message to the Dead_Letter_Queue.
6. THE SQS_Consumer SHALL use a configurable polling interval via `meme.queue.sqs.poll-interval` with a default of 10 seconds.
7. THE SQS_Consumer SHALL process messages sequentially to preserve deduplication correctness.

### Requirement 3: Kafka Producer — Publish Delivery Events

**User Story:** As a pipeline operator, I want a Kafka producer that publishes an event when a meme is persisted and ready for delivery, so that delivery consumers can react to new memes in real time instead of polling the database.

#### Acceptance Criteria

1. WHEN a meme is successfully persisted by the SQS_Consumer, THE Kafka_Producer SHALL publish a meme delivery event to the configured Kafka_Topic.
2. THE Kafka_Producer SHALL serialize the meme delivery event as JSON containing the meme database ID, title, image URL, and dankness score.
3. THE Kafka_Producer SHALL use the meme database ID as the Kafka message key to ensure ordering per meme.
4. IF the Kafka_Producer fails to publish an event, THEN THE Kafka_Producer SHALL log the error with the meme database ID and retry up to 3 times with exponential backoff.
5. THE Kafka_Producer SHALL be enabled or disabled via a `meme.stream.kafka.enabled` configuration property, defaulting to false.
6. WHILE the Kafka_Producer is disabled, THE Meme_Pipeline SHALL continue to use the existing scheduler-based delivery without modification.

### Requirement 4: Kafka Consumer — Trigger Meme Delivery

**User Story:** As a pipeline operator, I want a Kafka consumer that listens for meme delivery events and posts memes to Discord, so that delivery is event-driven and decoupled from the persistence layer.

#### Acceptance Criteria

1. WHEN a meme delivery event is received from the Kafka_Topic, THE Kafka_Consumer SHALL retrieve the full meme entity from the database using the meme database ID.
2. WHEN the meme entity is retrieved and has not been posted, THE Kafka_Consumer SHALL invoke the DiscordPosterService to post the meme to Discord.
3. WHEN the meme is successfully posted to Discord, THE Kafka_Consumer SHALL mark the meme as posted in the database and commit the Kafka offset.
4. IF the meme entity is not found in the database, THEN THE Kafka_Consumer SHALL log a warning with the meme database ID and skip the message.
5. IF the Discord posting fails, THEN THE Kafka_Consumer SHALL log the error and allow Kafka to redeliver the message based on the Consumer_Group retry policy.
6. THE Kafka_Consumer SHALL use a configurable Consumer_Group ID via `meme.stream.kafka.consumer-group` with a default of `dankposter-delivery`.

### Requirement 5: SQS and Kafka Configuration

**User Story:** As a pipeline operator, I want all SQS and Kafka connection details to be externalized as environment variables, so that the application can be configured for different environments without code changes.

#### Acceptance Criteria

1. THE Meme_Pipeline SHALL read the SQS queue URL from the `SQS_QUEUE_URL` environment variable.
2. THE Meme_Pipeline SHALL read the SQS dead-letter queue URL from the `SQS_DLQ_URL` environment variable.
3. THE Meme_Pipeline SHALL read the AWS region from the `AWS_REGION` environment variable with a default of `us-east-1`.
4. THE Meme_Pipeline SHALL read the Kafka bootstrap servers from the `KAFKA_BOOTSTRAP_SERVERS` environment variable with a default of `localhost:9092`.
5. THE Meme_Pipeline SHALL read the Kafka topic name from the `KAFKA_TOPIC` environment variable with a default of `meme-delivery`.
6. THE Meme_Pipeline SHALL bind all SQS and Kafka properties using `@ConfigurationProperties` classes consistent with the existing configuration pattern.

### Requirement 6: Meme Message Serialization Round-Trip

**User Story:** As a developer, I want meme messages to survive serialization and deserialization without data loss, so that the pipeline processes memes accurately across SQS and Kafka boundaries.

#### Acceptance Criteria

1. THE SQS_Producer SHALL serialize Meme_Message objects to JSON using a consistent serializer.
2. THE SQS_Consumer SHALL deserialize JSON Meme_Message payloads back into Meme_Message objects using the same serializer configuration.
3. FOR ALL valid Meme_Message objects, serializing to JSON then deserializing back SHALL produce an equivalent Meme_Message object (round-trip property).
4. THE Kafka_Producer SHALL serialize meme delivery events to JSON using a consistent serializer.
5. THE Kafka_Consumer SHALL deserialize JSON meme delivery events using the same serializer configuration.
6. FOR ALL valid meme delivery events, serializing to JSON then deserializing back SHALL produce an equivalent meme delivery event object (round-trip property).

### Requirement 7: Feature Toggle Coexistence

**User Story:** As a pipeline operator, I want to enable SQS and Kafka independently or together, so that I can adopt the integrations incrementally without breaking the existing pipeline.

#### Acceptance Criteria

1. WHILE `meme.queue.sqs.enabled` is false and `meme.stream.kafka.enabled` is false, THE Meme_Pipeline SHALL operate using the existing scheduler-based fetch-persist-post flow.
2. WHILE `meme.queue.sqs.enabled` is true and `meme.stream.kafka.enabled` is false, THE Meme_Pipeline SHALL enqueue fetched memes to SQS and consume them for persistence, but deliver memes using the existing scheduler-based posting.
3. WHILE `meme.queue.sqs.enabled` is true and `meme.stream.kafka.enabled` is true, THE Meme_Pipeline SHALL enqueue fetched memes to SQS, consume and persist them, publish delivery events to Kafka, and deliver memes via the Kafka_Consumer.
4. WHILE `meme.queue.sqs.enabled` is false and `meme.stream.kafka.enabled` is true, THE Meme_Pipeline SHALL use the existing direct-persistence flow for fetching and persist memes directly, but publish delivery events to Kafka and deliver memes via the Kafka_Consumer.
5. THE Meme_Pipeline SHALL use `@ConditionalOnProperty` annotations to conditionally load SQS and Kafka beans based on their respective feature toggle values.
