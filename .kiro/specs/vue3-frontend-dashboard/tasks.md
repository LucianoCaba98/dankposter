# Implementation Plan: Vue 3 Frontend Dashboard

## Overview

Build a Vue 3 SPA dashboard served from the Spring Boot backend. Backend-first approach: create the DTO, SSE infrastructure, REST controllers, and config API, then scaffold the Vue 3 frontend with Vite, wire up views and components, and integrate real-time SSE feeds with the admin panel.

## Tasks

- [x] 1. Create MemeDto and backend SSE infrastructure
  - [x] 1.1 Create `MemeDto` record in `com.shitpostengine.dank.dto`
    - Fields: `id`, `redditId`, `title`, `imageUrl`, `danknessScore`, `posted`, `description`
    - Add a static factory method `fromEntity(Meme meme)` for conversion
    - _Requirements: 2.4_

  - [x] 1.2 Create `SseEmitterService` in `com.shitpostengine.dank.service`
    - Manage `ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>` for channels `"ingestion"` and `"posted"`
    - `createEmitter(String channel)` — creates emitter with 30-min timeout, registers onCompletion/onTimeout/onError callbacks to remove from list
    - `broadcast(String channel, Object data)` — iterates emitters, sends data, catches per-emitter exceptions and removes dead ones
    - _Requirements: 7.2, 7.3, 7.4, 7.5_

  - [x] 1.3 Create `MemeEventPublisher` in `com.shitpostengine.dank.service`
    - `publishIngested(List<Meme> memes)` — converts each to `MemeDto`, broadcasts on `"ingestion"` channel
    - `publishPosted(Meme meme)` — converts to `MemeDto`, broadcasts on `"posted"` channel
    - _Requirements: 3.1, 4.1_

  - [x] 1.4 Hook `MemeEventPublisher` into existing services
    - Inject `MemeEventPublisher` into `RedditFetcherService`, call `publishIngested(savedMemes)` after `memeRepository.saveAll()`
    - Inject `MemeEventPublisher` into `DiscordPosterService`, call `publishPosted(meme)` after `meme.setPosted(true)` and `memeRepository.save(meme)`
    - _Requirements: 3.1, 4.1_

  - [ ]* 1.5 Write property tests for SseEmitterService
    - **Property 8: SSE emitter cleanup on completion**
    - **Validates: Requirements 7.3, 7.4**

  - [ ]* 1.6 Write property test for MemeEventPublisher broadcast
    - **Property 4: Meme lifecycle event broadcast**
    - **Validates: Requirements 3.1, 4.1, 7.5**

- [x] 2. Create REST controllers for meme data and SSE endpoints
  - [x] 2.1 Add `findByPostedTrue` paginated query to `MemeRepository`
    - `Page<Meme> findByPostedTrueOrderByIdDesc(Pageable pageable)`
    - `Page<Meme> findAllByOrderByIdDesc(Pageable pageable)`
    - _Requirements: 2.1, 2.2_

  - [x] 2.2 Create `MemeController` in `com.shitpostengine.dank.controller`
    - `GET /api/memes/posted?page=0&size=20` — returns `Page<MemeDto>` of posted memes, ordered by id desc
    - `GET /api/memes/all?page=0&size=20` — returns `Page<MemeDto>` of all memes, ordered by id desc
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 2.3 Create `SseController` in `com.shitpostengine.dank.controller`
    - `GET /api/events/ingestion` — returns `SseEmitter` from `SseEmitterService.createEmitter("ingestion")`
    - `GET /api/events/posted` — returns `SseEmitter` from `SseEmitterService.createEmitter("posted")`
    - _Requirements: 3.2, 4.2_

  - [ ]* 2.4 Write property tests for MemeController pagination
    - **Property 1: Posted memes filter**
    - **Property 2: All memes ordering**
    - **Property 3: Pagination bounds**
    - **Validates: Requirements 2.1, 2.2, 2.3**

- [x] 3. Create ConfigService and ConfigController
  - [x] 3.1 Create `ConfigService` in `com.shitpostengine.dank.service`
    - Inject all `@ConfigurationProperties` beans: `SqsProperties`, `KafkaProperties`, `RedditProperties`, `DiscordConfig`
    - Read scheduling properties from `@Value` fields or create a `SchedulingProperties` bean
    - `getAllConfig()` — returns `Map<String, Object>` grouped by category
    - `updateCategory(String category, Map<String, Object> values)` — validates, applies to beans via setters, returns updated values
    - Validation: reject negative durations, empty required strings when feature enabled, subreddit limit < 1
    - _Requirements: 8.1, 8.2, 8.3, 8.5, 8.6_

  - [x] 3.2 Create `ConfigController` in `com.shitpostengine.dank.controller`
    - `GET /api/config` — delegates to `ConfigService.getAllConfig()`
    - `PUT /api/config/{category}` — delegates to `ConfigService.updateCategory()`, returns 200 on success, 400 on validation failure, 404 on unknown category
    - _Requirements: 8.1, 8.3, 8.4, 8.5_

  - [ ]* 3.3 Write property tests for ConfigService
    - **Property 9: Configuration round-trip**
    - **Property 10: Invalid configuration rejection**
    - **Validates: Requirements 8.3, 8.4, 8.5, 8.6**

- [x] 4. Checkpoint — Backend complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Scaffold Vue 3 frontend with Vite
  - [x] 5.1 Initialize Vue 3 + TypeScript project in `frontend/`
    - Run `npm create vite@latest frontend -- --template vue-ts`
    - Install dependencies: `vue-router`, `fast-check` (dev)
    - Configure `vite.config.ts` to output build to `../src/main/resources/static/`
    - Configure dev server proxy: `/api` → `http://localhost:8080`
    - _Requirements: 1.1, 1.2_

  - [x] 5.2 Create TypeScript interfaces in `frontend/src/types.ts`
    - `Meme`, `ConfigCategory`, `AppConfig`, `ValidationError` interfaces matching the design
    - _Requirements: 2.4_

  - [x] 5.3 Set up Vue Router in `frontend/src/router.ts`
    - Hash mode (`createWebHashHistory`)
    - Routes: `/` → HomeFeed, `/ingestion` → IngestionFeed, `/admin` → AdminPanel
    - Default route to HomeFeed
    - _Requirements: 1.4, 6.2, 6.3, 6.4, 6.5_

  - [x] 5.4 Create `App.vue` with `NavBar.vue` and `<router-view>`
    - NavBar with three tabs: "Home", "Meme Fetching", "Admin"
    - Active tab indicator using `router-link-active` class
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 6. Create composables and reusable components
  - [x] 6.1 Create `useSse` composable in `frontend/src/composables/useSse.ts`
    - Accepts URL, returns reactive `lastEvent` ref
    - Manages EventSource lifecycle: open, message parsing, error handling
    - Auto-closes on component unmount (`onUnmounted`)
    - _Requirements: 3.4, 4.4, 7.1_

  - [x] 6.2 Create `useNotification` composable in `frontend/src/composables/useNotification.ts`
    - Manages toast notifications (success/error)
    - Auto-dismiss after configurable duration (default 3s for success)
    - _Requirements: 10.3, 10.4, 10.5_

  - [x] 6.3 Create `MemeCard.vue` component
    - Display title, image (`<img>` with `@error` fallback to placeholder), dankness score (1 decimal), posted badge, truncated description (140 chars + ellipsis)
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [ ]* 6.4 Write property tests for MemeCard and truncation
    - **Property 6: MemeCard renders all required fields**
    - **Property 7: Description truncation**
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.6**

- [x] 7. Implement feed views
  - [x] 7.1 Create `HomeFeed.vue`
    - Fetch posted memes from `GET /api/memes/posted` on mount
    - Subscribe to `/api/events/posted` SSE via `useSse`
    - Prepend new SSE events to top of feed
    - Render memes as `MemeCard` list
    - _Requirements: 2.1, 4.2, 4.5, 5.1_

  - [x] 7.2 Create `IngestionFeed.vue`
    - Fetch all memes from `GET /api/memes/all` on mount
    - Subscribe to `/api/events/ingestion` SSE via `useSse`
    - Prepend new SSE events to top of feed
    - Render memes as `MemeCard` list
    - _Requirements: 2.2, 3.2, 3.5, 5.1_

  - [ ]* 7.3 Write property test for feed prepend ordering
    - **Property 5: Feed prepend ordering**
    - **Validates: Requirements 3.5, 4.5**

- [x] 8. Implement Admin Panel
  - [x] 8.1 Create `ConfigField.vue` component
    - Render toggle for booleans, number input for numeric/duration, text input for strings
    - Inline validation with error messages below field
    - Emit `update:modelValue` for v-model binding
    - _Requirements: 9.3, 9.4, 9.5, 11.4_

  - [x] 8.2 Create `ConfigSection.vue` component
    - Render category heading with list of `ConfigField` components
    - "Save" button per section, disabled when any field fails validation or save in progress
    - Loading indicator while save request is in flight
    - _Requirements: 9.1, 10.1, 10.6, 11.5_

  - [x] 8.3 Create `AdminPanel.vue` view
    - Fetch config from `GET /api/config` on mount
    - Render `ConfigSection` for each category: Scheduling, SQS, Kafka, Meme Sources, Meme Posting, Reddit Subreddits
    - Reddit Subreddits rendered as editable list with name + limit per entry
    - Save sends `PUT /api/config/{category}`, shows success/error notification via `useNotification`
    - Error state with "Failed to load configuration" message and retry button
    - _Requirements: 8.1, 9.1, 9.2, 9.6, 9.7, 10.2, 10.3, 10.4, 10.5_

  - [ ]* 8.4 Write property tests for config validation
    - **Property 11: Config field type rendering**
    - **Property 12: Duration and numeric fields require positive values**
    - **Property 13: Conditional required field validation**
    - **Property 14: Subreddit entry validation**
    - **Property 15: Validation state disables save**
    - **Validates: Requirements 9.3, 9.4, 9.5, 11.1, 11.2, 11.3, 11.6, 11.4, 11.5**

- [x] 9. Checkpoint — Frontend complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Integration wiring and final polish
  - [x] 10.1 Verify Vite build outputs to `src/main/resources/static/`
    - Run `npm run build` in `frontend/`, confirm `index.html` and assets land in static dir
    - Verify Spring Boot serves the SPA at root URL `/`
    - _Requirements: 1.2, 1.3_

  - [x] 10.2 Verify SSE end-to-end flow
    - Confirm ingestion events flow from `RedditFetcherService` → `MemeEventPublisher` → `SseEmitterService` → `SseController` → browser EventSource
    - Confirm posted events flow from `DiscordPosterService` → `MemeEventPublisher` → `SseEmitterService` → `SseController` → browser EventSource
    - _Requirements: 3.1, 3.2, 3.3, 4.1, 4.2, 4.3_

  - [x] 10.3 Verify config round-trip
    - Confirm `GET /api/config` returns all categories with current values
    - Confirm `PUT /api/config/{category}` updates in-memory beans and subsequent GET reflects changes
    - _Requirements: 8.3, 8.4, 8.6_

- [-] 11. Final checkpoint — All integration verified
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Backend tasks (1–4) should be completed before frontend tasks (5–9)
- The Vue 3 app lives in `frontend/` at project root; Vite builds to `src/main/resources/static/`
