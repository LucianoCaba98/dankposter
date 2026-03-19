---
inclusion: always
---

# Coding Standards

## Language & Framework

- Java 17 — use records where appropriate, pattern matching for instanceof, text blocks
- Spring Boot 3.5 — prefer constructor injection, use `@ConfigurationProperties` for config binding
- Lombok — use `@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j` consistently

## Naming Conventions

- Packages: lowercase, no underscores (e.g., `externalIntegrations` is the existing convention — follow it)
- Classes: PascalCase. DTOs suffixed with their API origin (e.g., `RedditPost`, `GiphyGif`)
- Interfaces: no `I` prefix. Use descriptive names (e.g., `MemeSource`, `MemeRenderer`)
- Enums: UPPER_SNAKE_CASE values

## Reactive Code

- Use Project Reactor (`Mono`, `Flux`) for all external I/O
- Wrap blocking JPA calls with `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`
- Always handle errors with `.onErrorResume()` — never let exceptions propagate silently in reactive chains
- Use `concatMap` for ordered sequential processing, `flatMap` for concurrent

## Configuration

- All secrets and environment-specific values go in `application.yml` with `${ENV_VAR:default}` syntax
- Never hardcode API keys, tokens, or URLs
- Use `@ConditionalOnProperty` for feature toggles (e.g., enabling/disabling meme sources)

## Error Handling

- Create specific exception classes in `model.error` package
- Log errors with context (meme ID, source name, etc.)
- Use appropriate log levels: `debug` for duplicates, `warn` for recoverable issues, `error` for failures

## Testing

- Tests go in `src/test/java/com/dankposter/` mirroring the main source structure
- Use JUnit 5 + Spring Boot Test
- Mock external APIs with `@MockBean` or WireMock
- Test reactive chains with `StepVerifier`
