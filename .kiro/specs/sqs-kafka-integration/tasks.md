# Implementation Plan: SQS-Kafka Integration

## Overview

Incrementally introduce AWS SQS and Apache Kafka into the DankPoster meme pipeline. Each task builds on the previous, starting with dependencies and configuration, then SQS producer/consumer, then Kafka producer/consumer, then wiring the modified existing components, and finally integration validation. All new beans are conditionally loaded via `@ConditionalOnProperty` so the existing flow remains the default.

## Tasks

- [x] 1. Add dependencies and create DTO records
  - [x] 1.1 Add Maven dependencies for Spring Cloud AWS SQS, Spring Kafka, and jqwik
    - Add `io.awspring.cloud:spring-cloud-aws-starter-sqs` to `pom.xml`
    - Add `org.springframework.kafka:spring-kafka` to `pom.xml`
    - Add `net.jqwik:jqwik:1.9.1` to `pom.xml` (test scope)
    - _Requirements: 1.1, 3.1, 6.1, 6.4_

  - [x] 1.2 Create `MemeMessage` record
    - Create `src/main/java/com/shitpostengine/dank/dto/MemeMessage.java`
    - Java record with fields: `title`, `imageUrl`, `sourceIdentifier`, `sourceSpecificId`, `danknessScore`
    - _Requirements: 1.2, 6.1, 6.2_

  - [x] 1.3 Create `MemeDeliveryEvent` record
    - Create `src/main/java/com/shitpostengine/dank/dto/MemeDeliveryEvent.java`
    - Java record with fields: `memeId`, `title`, `imageUrl`, `danknessScore`
    - _Requirements: 3.2, 6.4, 6.5_

  - [ ]* 1.4 Write property test for MemeMessage serialization round-trip
    - **Property 1: MemeMessage serialization round-trip**
    - **Validates: Requirements 1.2, 6.1, 6.2, 6.3**
    - Create `src/test/java/com/shitpostengine/dank/dto/MemeMessagePropertyTest.java`
    - Use jqwik `@Property` with custom `Arbitrary<MemeMessage>` generator
    - Assert Jackson serialize → deserialize produces equal object

  - [ ]* 1.5 Write property test for MemeDeliveryEvent serialization round-trip
    - **Property 2: MemeDeliveryEvent serialization round-trip**
    - **Validates: Requirements 3.2, 6.4, 6.5, 6.6**
    - Create `src/test/java/com/shitpostengine/dank/dto/MemeDeliveryEventPropertyTest.java`
    - Use jqwik `@Property` with custom `Arbitrary<MemeDeliveryEvent>` generator
    - Assert Jackson serialize → deserialize produces equal object

- [x] 2. Create configuration properties and auto-configuration classes
  - [x] 2.1 Create `SqsProperties` configuration class
    - Create `src/main/java/com/shitpostengine/dank/config/SqsProperties.java`
    - `@ConfigurationProperties(prefix = "meme.queue.sqs")` with fields: `enabled`, `queueUrl`, `dlqUrl`, `region`, `pollInterval`
    - Defaults: `enabled=false`, `region=us-east-1`, `pollInterval=10s`
    - _Requirements: 1.4, 5.1, 5.2, 5.3, 5.6_

  - [x] 2.2 Create `KafkaProperties` configuration class
    - Create `src/main/java/com/shitpostengine/dank/config/KafkaProperties.java`
    - `@ConfigurationProperties(prefix = "meme.stream.kafka")` with fields: `enabled`, `bootstrapServers`, `topic`, `consumerGroup`
    - Defaults: `enabled=false`, `bootstrapServers=localhost:9092`, `topic=meme-delivery`, `consumerGroup=dankposter-delivery`
    - _Requirements: 3.5, 4.6, 5.4, 5.5, 5.6_

  - [x] 2.3 Add SQS and Kafka configuration to `application.yml`
    - Add `meme.queue.sqs.*` and `meme.stream.kafka.*` sections with `${ENV_VAR:default}` bindings
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 2.4 Create `SqsAutoConfiguration` class
    - Create `src/main/java/com/shitpostengine/dank/config/SqsAutoConfiguration.java`
    - `@Configuration` + `@ConditionalOnProperty(name = "meme.queue.sqs.enabled", havingValue = "true")`
    - Register `SqsClient` and `SqsTemplate` beans using `SqsProperties`
    - _Requirements: 1.4, 7.5_

  - [x] 2.5 Create `KafkaAutoConfiguration` class
    - Create `src/main/java/com/shitpostengine/dank/config/KafkaAutoConfiguration.java`
    - `@Configuration` + `@ConditionalOnProperty(name = "meme.stream.kafka.enabled", havingValue = "true")`
    - Configure `ProducerFactory`, `ConsumerFactory`, `KafkaTemplate` beans using `KafkaProperties`
    - Configure producer retries (3 attempts, exponential backoff)
    - _Requirements: 3.4, 3.5, 7.5_

  - [ ]* 2.6 Write unit tests for configuration binding and feature toggles
    - Verify `SqsProperties` and `KafkaProperties` bind correctly from YAML with defaults
    - Verify `@ConditionalOnProperty` loads/excludes beans for all 4 toggle combinations
    - _Requirements: 5.1–5.6, 7.1–7.5_

- [x] 3. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement SQS producer and consumer services
  - [x] 4.1 Implement `SqsProducerService`
    - Create `src/main/java/com/shitpostengine/dank/service/SqsProducerService.java`
    - `@ConditionalOnProperty(name = "meme.queue.sqs.enabled", havingValue = "true")`
    - Inject `SqsTemplate` and `ObjectMapper`
    - `void send(MemeMessage message)` — serialize to JSON, send to queue URL
    - On failure: log error with `sourceSpecificId`, do not throw
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 4.2 Implement `SqsConsumerService`
    - Create `src/main/java/com/shitpostengine/dank/service/SqsConsumerService.java`
    - `@ConditionalOnProperty(name = "meme.queue.sqs.enabled", havingValue = "true")`
    - Poll SQS queue on scheduled interval from `SqsProperties.pollInterval`
    - Deserialize `MemeMessage` from JSON
    - Deduplicate via `MemeRepository.existsByRedditId(sourceSpecificId)`
    - Persist new memes, delete processed messages from queue
    - Duplicate messages: acknowledge (delete) and log at debug level
    - After persist: delegate to `KafkaProducerService` if Kafka enabled (via `Optional<KafkaProducerService>`)
    - Sequential processing for deduplication correctness
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

  - [ ]* 4.3 Write property test for deduplication preserves uniqueness
    - **Property 3: Deduplication preserves uniqueness**
    - **Validates: Requirements 2.2, 2.7**
    - Create `src/test/java/com/shitpostengine/dank/service/SqsConsumerServicePropertyTest.java`
    - Generate random lists of `MemeMessage` with controlled duplicate `sourceSpecificId` values
    - Assert persisted meme count equals distinct `sourceSpecificId` count

  - [ ]* 4.4 Write unit tests for SQS producer and consumer
    - Test `SqsProducerService.send()` calls SqsTemplate for each meme
    - Test error logging on SQS send failure without halting
    - Test duplicate meme is acknowledged but not persisted
    - Test message deletion after successful persist
    - _Requirements: 1.3, 2.3, 2.4_

- [x] 5. Implement Kafka producer and consumer services
  - [x] 5.1 Implement `KafkaProducerService`
    - Create `src/main/java/com/shitpostengine/dank/service/KafkaProducerService.java`
    - `@ConditionalOnProperty(name = "meme.stream.kafka.enabled", havingValue = "true")`
    - Inject `KafkaTemplate<String, String>` and `ObjectMapper`
    - `void publishDeliveryEvent(MemeDeliveryEvent event)` — serialize to JSON, send with `String.valueOf(memeId)` as key
    - Retry handled by Kafka producer config (3 attempts, exponential backoff)
    - On failure after retries: log error with `memeId`
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 5.2 Implement `KafkaConsumerService`
    - Create `src/main/java/com/shitpostengine/dank/service/KafkaConsumerService.java`
    - `@ConditionalOnProperty(name = "meme.stream.kafka.enabled", havingValue = "true")`
    - `@KafkaListener` consuming from configured topic and consumer group
    - Deserialize `MemeDeliveryEvent` from JSON
    - Look up `Meme` by `memeId` from `MemeRepository`
    - If not found: log warning, skip
    - If already posted: skip
    - Otherwise: invoke `DiscordPosterService` to post, mark as posted, commit offset manually
    - On Discord failure: log error, do not commit offset
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [ ]* 5.3 Write property test for Kafka message key matches meme ID
    - **Property 4: Kafka message key matches meme database ID**
    - **Validates: Requirements 3.3**
    - Create `src/test/java/com/shitpostengine/dank/service/KafkaProducerServicePropertyTest.java`
    - Generate random `MemeDeliveryEvent` records
    - Assert key passed to `KafkaTemplate.send()` equals `String.valueOf(event.memeId())`

  - [ ]* 5.4 Write property test for successful delivery marks meme as posted
    - **Property 5: Successful delivery marks meme as posted**
    - **Validates: Requirements 4.3**
    - Create `src/test/java/com/shitpostengine/dank/service/KafkaConsumerServicePropertyTest.java`
    - Generate random `Meme` entities with `posted = false`
    - Assert after successful delivery processing, `meme.isPosted() == true`

  - [ ]* 5.5 Write unit tests for Kafka producer and consumer
    - Test retry behavior on publish failure
    - Test meme-not-found logs warning and skips
    - Test Discord failure does not commit offset
    - Test already-posted meme is skipped without re-posting
    - _Requirements: 3.4, 4.4, 4.5_

- [x] 6. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Modify existing components for integration
  - [x] 7.1 Modify `RedditFetcherService` to delegate to SQS when enabled
    - Inject `Optional<SqsProducerService>` into `RedditFetcherService`
    - When SQS is enabled: convert scored memes to `MemeMessage` and call `SqsProducerService.send()` instead of persisting directly
    - When SQS is disabled: existing behavior unchanged
    - _Requirements: 1.1, 1.4, 1.5, 7.2, 7.4_

  - [x] 7.2 Modify `MemeScheduler` to conditionally disable poster when Kafka is enabled
    - The `memePoster()` scheduler should only run when Kafka is disabled
    - Use `@ConditionalOnProperty(name = "meme.stream.kafka.enabled", havingValue = "false", matchIfMissing = true)` or a runtime check
    - _Requirements: 3.6, 7.1, 7.3_

  - [x] 7.3 Wire `KafkaProducerService` into `SqsConsumerService` for post-persist event publishing
    - After successful meme persist in `SqsConsumerService`, call `KafkaProducerService.publishDeliveryEvent()` if Kafka is enabled
    - Also wire into direct-persist path (when SQS disabled but Kafka enabled) so that `RedditFetcherService` triggers Kafka events after DB save
    - _Requirements: 3.1, 7.3, 7.4_

  - [ ]* 7.4 Write unit tests for modified existing components
    - Test `RedditFetcherService` delegates to SQS when enabled, persists directly when disabled
    - Test `MemeScheduler.memePoster()` is inactive when Kafka is enabled
    - Test Kafka events are published after persist in both SQS-enabled and SQS-disabled paths
    - _Requirements: 1.4, 1.5, 3.6, 7.1–7.4_

- [x] 8. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- All new beans use `@ConditionalOnProperty` so the existing pipeline is unaffected when toggles are off
