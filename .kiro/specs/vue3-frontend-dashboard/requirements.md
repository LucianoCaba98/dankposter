# Requirements Document

## Introduction

DankPoster is currently a headless meme ingestion and distribution pipeline. This feature adds a Vue 3 single-page application (SPA) frontend served from the existing Spring Boot backend. The dashboard provides three views: a "Home" tab showing memes that have been posted to Discord, a "Meme Fetching" tab showing memes as they are ingested and stored to the database, and an "Admin" tab for viewing and editing all runtime-configurable application settings. Real-time updates are delivered via Server-Sent Events (SSE) so the user can watch the pipeline operate live without refreshing.

## Glossary

- **Dashboard**: The Vue 3 single-page application served by the Spring Boot backend at the root URL
- **Home_Feed**: The view displaying memes that have been posted to Discord, ordered by most recent
- **Ingestion_Feed**: The view displaying memes as they are fetched from Reddit and stored to the database, ordered by most recent
- **Meme_Card**: A UI component that renders a single meme's title, image, dankness score, and posted status
- **SSE_Endpoint**: A Spring Boot controller endpoint that emits Server-Sent Events to push real-time meme data to the frontend
- **REST_API**: Spring Boot REST controllers that serve paginated meme data for initial page loads
- **Frontend_Router**: The Vue Router instance managing tab-based navigation between Home_Feed, Ingestion_Feed, and Admin_Panel
- **Event_Emitter**: A backend service that publishes meme lifecycle events (ingested, posted) to SSE subscribers
- **Admin_Panel**: The view displaying all runtime-configurable application settings grouped by category, with controls to edit and persist changes
- **Config_API**: Spring Boot REST controller endpoints that serve current configuration values and accept updates at runtime
- **Config_Section**: A UI component grouping related configuration properties under a category heading (e.g., Scheduling, SQS, Kafka, Meme Sources, Meme Posting, Reddit Subreddits)
- **Config_Field**: A single editable configuration property rendered with an input type appropriate to its data type (toggle for booleans, number input for numeric values, text input for strings)

## Requirements

### Requirement 1: Vue 3 SPA Scaffold and Spring Boot Integration

**User Story:** As a developer, I want the Vue 3 frontend to be built and served from the Spring Boot static resources directory, so that the entire application runs as a single deployable unit on port 8080.

#### Acceptance Criteria

1. THE Dashboard SHALL be a Vue 3 application using Vite as the build tool
2. WHEN the Vue 3 application is built, THE build process SHALL output compiled assets to `src/main/resources/static/`
3. WHEN a user navigates to the root URL (`/`) on port 8080, THE Spring Boot server SHALL serve the Dashboard index.html
4. THE Dashboard SHALL use Vue Router for client-side tab navigation between Home_Feed, Ingestion_Feed, and Admin_Panel
5. IF the Vue 3 build artifacts are missing from the static directory, THEN THE Spring Boot server SHALL still start and serve its REST_API endpoints without error

### Requirement 2: REST API for Initial Meme Data

**User Story:** As a frontend consumer, I want REST endpoints that return paginated meme data, so that the dashboard can load existing memes on initial page render.

#### Acceptance Criteria

1. WHEN the Home_Feed is loaded, THE REST_API SHALL return a paginated list of memes where the `posted` field is `true`, ordered by `id` descending
2. WHEN the Ingestion_Feed is loaded, THE REST_API SHALL return a paginated list of all memes ordered by `id` descending
3. THE REST_API SHALL accept `page` and `size` query parameters for pagination, defaulting to page 0 and size 20
4. THE REST_API SHALL return meme data including `id`, `redditId`, `title`, `imageUrl`, `danknessScore`, `posted`, and `description` fields
5. IF no memes exist matching the query, THEN THE REST_API SHALL return an empty list with a 200 status code

### Requirement 3: Real-Time Ingestion Events via SSE

**User Story:** As a user, I want to see memes appear in the Ingestion_Feed in real-time as they are fetched and stored, so that I can watch the pipeline operate live.

#### Acceptance Criteria

1. WHEN a meme is persisted to the database during a fetch cycle, THE Event_Emitter SHALL publish an `ingested` event containing the saved meme data
2. THE SSE_Endpoint SHALL expose a `/api/events/ingestion` endpoint that streams `ingested` events to connected clients
3. WHILE a client is connected to the ingestion SSE_Endpoint, THE SSE_Endpoint SHALL push each new `ingested` event to the client within 2 seconds of persistence
4. IF the SSE connection is dropped, THEN THE Dashboard SHALL attempt to reconnect using the browser's native EventSource reconnection mechanism
5. WHEN the Ingestion_Feed view is active, THE Dashboard SHALL prepend each received `ingested` event as a new Meme_Card at the top of the feed

### Requirement 4: Real-Time Posted Meme Events via SSE

**User Story:** As a user, I want to see memes appear in the Home_Feed in real-time as they are posted to Discord, so that I can confirm delivery is working.

#### Acceptance Criteria

1. WHEN a meme is successfully posted to Discord and marked as `posted=true`, THE Event_Emitter SHALL publish a `posted` event containing the posted meme data
2. THE SSE_Endpoint SHALL expose a `/api/events/posted` endpoint that streams `posted` events to connected clients
3. WHILE a client is connected to the posted SSE_Endpoint, THE SSE_Endpoint SHALL push each new `posted` event to the client within 2 seconds of the meme being marked as posted
4. IF the SSE connection is dropped, THEN THE Dashboard SHALL attempt to reconnect using the browser's native EventSource reconnection mechanism
5. WHEN the Home_Feed view is active, THE Dashboard SHALL prepend each received `posted` event as a new Meme_Card at the top of the feed

### Requirement 5: Meme Card Display

**User Story:** As a user, I want each meme displayed as a visual card with its key information, so that I can quickly scan the feed.

#### Acceptance Criteria

1. THE Meme_Card SHALL display the meme's `title` as the card heading
2. THE Meme_Card SHALL display the meme's `imageUrl` as a rendered image
3. THE Meme_Card SHALL display the meme's `danknessScore` formatted to one decimal place
4. THE Meme_Card SHALL display a visual indicator showing whether the meme has been `posted` to Discord
5. IF the meme's `imageUrl` fails to load, THEN THE Meme_Card SHALL display a placeholder image with alt text "Meme image unavailable"
6. THE Meme_Card SHALL display the meme's `description` truncated to 140 characters with an ellipsis when the description exceeds that length

### Requirement 6: Tab Navigation

**User Story:** As a user, I want to switch between the Home, Meme Fetching, and Admin tabs, so that I can view different aspects of the pipeline and manage application settings.

#### Acceptance Criteria

1. THE Dashboard SHALL display a navigation bar with three tabs labeled "Home", "Meme Fetching", and "Admin"
2. WHEN the user clicks the "Home" tab, THE Frontend_Router SHALL navigate to the Home_Feed view and THE Dashboard SHALL visually indicate the active tab
3. WHEN the user clicks the "Meme Fetching" tab, THE Frontend_Router SHALL navigate to the Ingestion_Feed view and THE Dashboard SHALL visually indicate the active tab
4. WHEN the user clicks the "Admin" tab, THE Frontend_Router SHALL navigate to the Admin_Panel view and THE Dashboard SHALL visually indicate the active tab
5. WHEN the Dashboard is first loaded, THE Frontend_Router SHALL default to the Home_Feed view
6. THE Dashboard SHALL maintain separate scroll positions for each tab when switching between them

### Requirement 7: SSE Connection Lifecycle Management

**User Story:** As a developer, I want SSE connections to be properly managed, so that server resources are not leaked.

#### Acceptance Criteria

1. WHEN a client navigates away from a feed view, THE Dashboard SHALL close the corresponding SSE connection
2. THE SSE_Endpoint SHALL use Spring's `SseEmitter` with a configurable timeout defaulting to 30 minutes
3. IF an SSE_Endpoint emitter times out, THEN THE SSE_Endpoint SHALL cleanly remove the emitter from its subscriber list
4. IF an SSE_Endpoint emitter encounters an I/O error, THEN THE SSE_Endpoint SHALL log the error at `warn` level and remove the emitter from its subscriber list
5. THE Event_Emitter SHALL support multiple concurrent SSE subscribers per endpoint


### Requirement 8: Configuration REST API

**User Story:** As a frontend consumer, I want REST endpoints that return current configuration values and accept updates, so that the Admin_Panel can read and modify application settings at runtime.

#### Acceptance Criteria

1. WHEN the Admin_Panel is loaded, THE Config_API SHALL expose a `GET /api/config` endpoint that returns all current configuration values grouped by category
2. THE Config_API SHALL return configuration values in the following categories: Scheduling, SQS, Kafka, Meme_Sources, Meme_Posting, and Reddit_Subreddits
3. WHEN a valid configuration update is submitted, THE Config_API SHALL expose a `PUT /api/config/{category}` endpoint that applies the updated values to the running application
4. WHEN a configuration update is applied successfully, THE Config_API SHALL return a 200 status code with the updated configuration values for the changed category
5. IF a configuration update contains an invalid value (e.g., negative duration, empty required string), THEN THE Config_API SHALL return a 400 status code with a descriptive error message identifying the invalid field
6. THE Config_API SHALL update the in-memory `@ConfigurationProperties` beans so that changes take effect without requiring an application restart

### Requirement 9: Admin Panel View

**User Story:** As an operator, I want an administration panel in the dashboard where I can see all configurable values grouped by category, so that I can understand and manage the application's runtime behavior.

#### Acceptance Criteria

1. THE Admin_Panel SHALL display all configuration values organized into Config_Section components for each category: Scheduling, SQS, Kafka, Meme Sources, Meme Posting, and Reddit Subreddits
2. WHEN the Admin_Panel is loaded, THE Admin_Panel SHALL fetch current configuration values from the Config_API and populate each Config_Field with the current value
3. THE Admin_Panel SHALL render boolean configuration properties as toggle switches
4. THE Admin_Panel SHALL render numeric and duration configuration properties as number input fields with the current value displayed
5. THE Admin_Panel SHALL render string configuration properties as text input fields with the current value displayed
6. THE Admin_Panel SHALL render the Reddit Subreddits category as an editable list where each entry displays a subreddit name and limit
7. IF the Config_API request fails on load, THEN THE Admin_Panel SHALL display an error message "Failed to load configuration" with a retry button

### Requirement 10: Configuration Save and Feedback

**User Story:** As an operator, I want to save configuration changes and receive clear feedback on success or failure, so that I can confidently manage runtime settings.

#### Acceptance Criteria

1. THE Admin_Panel SHALL display a "Save" button within each Config_Section that submits only the values for that category to the Config_API
2. WHEN the user clicks a Config_Section "Save" button, THE Admin_Panel SHALL send a `PUT` request to `Config_API` with the updated values for that category
3. WHEN the Config_API returns a 200 status code, THE Admin_Panel SHALL display a success notification with the text "Settings saved" that auto-dismisses after 3 seconds
4. IF the Config_API returns a 400 status code, THEN THE Admin_Panel SHALL display an error notification containing the error message returned by the Config_API
5. IF the Config_API returns a 500 status code or the request fails due to a network error, THEN THE Admin_Panel SHALL display an error notification with the text "Failed to save settings. Please try again."
6. WHILE a save request is in progress, THE Admin_Panel SHALL disable the corresponding "Save" button and display a loading indicator

### Requirement 11: Configuration Field Validation

**User Story:** As an operator, I want the admin panel to validate my input before submitting, so that I do not accidentally save invalid configuration values.

#### Acceptance Criteria

1. THE Admin_Panel SHALL validate that duration fields (fetch-interval-ms, post-interval-ms, poll-interval, posting interval, fetch-interval) contain positive numeric values before submission
2. THE Admin_Panel SHALL validate that the SQS queue-url and dlq-url fields are non-empty strings when SQS is enabled
3. THE Admin_Panel SHALL validate that the Kafka bootstrap-servers field is a non-empty string when Kafka is enabled
4. IF a Config_Field fails client-side validation, THEN THE Admin_Panel SHALL display an inline error message below the field describing the validation rule
5. WHILE any Config_Field within a Config_Section fails validation, THE Admin_Panel SHALL disable the "Save" button for that Config_Section
6. THE Admin_Panel SHALL validate that each Reddit subreddit entry has a non-empty name and a limit value of at least 1
