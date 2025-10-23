# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Monito** is a containerized infrastructure monitoring platform built with Spring Boot 3. It collects metrics from Docker containers via external monitoring agents, stores time-series data, applies alert rules, and delivers real-time notifications via WebSocket.

- **Language:** Java 17
- **Framework:** Spring Boot 3.4.10
- **Database:** Oracle (via JDBC)
- **Architecture:** Domain-Driven Design with layered architecture
- **Authentication:** Stateless JWT (access + refresh tokens)
- **Real-time:** WebSocket for alert notifications

## Build and Run Commands

### Build the project
```bash
./gradlew build
```

### Run the application
```bash
./gradlew bootRun
```

### Run tests
```bash
./gradlew test
```

### Run a single test class
```bash
./gradlew test --tests com.monito.domains.auth.service.AuthServiceImplTest
```

### Run a single test method
```bash
./gradlew test --tests "com.monito.domains.auth.service.AuthServiceImplTest.testLogin"
```

### Clean build artifacts
```bash
./gradlew clean
```

### Generate OpenAPI documentation
The API documentation is automatically available at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Environment Configuration

The application requires the following environment variables (typically in `.env` file, loaded via `spring-dotenv`):

```properties
DB_URL=jdbc:oracle:thin:@hostname:port:sid
DB_USERNAME=your_username
DB_PASSWORD=your_password
JWT_SECRET=your_secret_key_minimum_32_characters
```

**Note:** The `.env` file is git-ignored. Never commit it to the repository.

## Architecture Overview

### Domain Structure

The codebase follows Domain-Driven Design with these core domains:

1. **Agent Domain** (`domains/agent/`)
   - Manages external monitoring agents that collect container metrics
   - Polling scheduler fetches data every 5 seconds from agent APIs
   - Health tracking transitions agents to OFFLINE after 3 consecutive failures
   - Key classes: `Agent`, `AgentPollingScheduler`, `AgentHealthTracker`

2. **Container Domain** (`domains/container/`)
   - Stores Docker container metrics (CPU, memory, network, I/O)
   - Time-series data in `ContainerStatsLog` for historical analysis
   - Async collection via `ContainerCollectorServiceImpl`
   - Comprehensive metrics: CPU throttling, memory RSS/cache, network Mbps/PPS, block I/O

3. **Alert Domain** (`domains/alert/`)
   - Real-time alerting system with configurable rules
   - Alert levels: CRITICAL, WARNING, INFO
   - WebSocket push notifications to connected users
   - Key classes: `Alert`, `AlertRule`, `AlertService`, `AlertWebSocketHandler`

4. **Member Domain** (`domains/member/`)
   - User management with roles (USER, ADMIN)
   - Password encryption via BCrypt
   - Soft-delete support

5. **Auth Domain** (`domains/auth/`)
   - JWT-based authentication (access + refresh tokens)
   - Token expiration: 24 hours (access), 7 days (refresh)
   - Key classes: `AuthServiceImpl`, `JwtTokenProvider`, `JwtAuthenticationFilter`

6. **Dashboard Domain** (`domains/dashboard/`)
   - User-customizable monitoring dashboards
   - Widget-based layout with templates
   - Relationships: Dashboard → DashboardWidget → WidgetTemplate

7. **Favorite Domain** (`domains/favorite/`)
   - Users can mark containers/agents as favorites

### Key Architectural Patterns

- **Async Processing:** `@Async` + `ThreadPoolTaskExecutor` for agent polling (5 core threads, 20 max)
- **Reactive HTTP:** `WebClient` for non-blocking agent API calls (3s connection timeout, 5s read/write)
- **Soft Deletes:** `@SQLRestriction("is_deleted = 0")` on entities
- **Audit Trail:** `BaseEntity` with `@CreatedDate` and `@LastModifiedDate`
- **Repository Pattern:** Spring Data JPA repositories per domain
- **WebSocket Push:** Real-time alert delivery via `AlertWebSocketHandler`

### Agent Integration Flow

```
AgentPollingScheduler (every 5s)
  → Fetch all ONLINE agents
  → For each agent (async/parallel):
      → ContainerCollectorServiceImpl.collectContainerData()
      → HTTP GET to http://{agentIp}:{port}/api/containers (Bearer token auth)
      → Parse metrics & update Container + ContainerStatsLog
      → AgentHealthTracker records success/failure
      → After 3 failures → Agent status = OFFLINE
```

### WebSocket Architecture

- **Endpoint:** `/ws/alerts?userId={userId}`
- **Handler:** `AlertWebSocketHandler` maintains `ConcurrentHashMap<String, WebSocketSession>`
- **Message Flow:** Alert created → `AlertService` → `sendAlertToUser(userId, json)` → WebSocket session
- **Message Format:** JSON with `type`, `title`, `message`, `timestamp`, `data`
- **Security:** WebSocket endpoints bypass JWT authentication (userId from query parameter)

### Security Configuration

- **Authentication:** Stateless JWT with `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
- **Public Endpoints:** `/api/auth/**`, `/api/test/public`, `/ws/**`, `/swagger-ui/**`, `/h2-console/**`
- **Protected Endpoints:** `/api/admin/**` requires ADMIN role
- **CORS:** Configured to allow all origins (configure for production)
- **CSRF:** Disabled (stateless JWT approach)
- **Password Encoding:** BCrypt

### Database Schema

All entities use Oracle SEQUENCE for ID generation:
- `AGENT_SEQ`, `CONTAINER_SEQ`, `MEMBER_SEQ`, `ALERT_SEQ`, etc.

Key relationships:
- `Agent` (1) → (Many) `Container`
- `Container` (1) → (Many) `ContainerStatsLog`
- `AlertRule` (1) → (Many) `Alert`
- `Member` (1) → (Many) `Alert`, `Dashboard`, `Favorite`
- `Dashboard` (1) → (Many) `DashboardWidget` (CASCADE ALL, orphan removal)

Fetch strategy: Lazy loading by default to prevent N+1 queries.

## Development Guidelines

### Adding a New Domain

1. Create package structure: `domains/{domain}/`
   - `domain/` - Entities and enums
   - `dto/request/` and `dto/response/` - Data transfer objects
   - `repository/` - Spring Data JPA repositories
   - `service/` - Business logic (interface + implementation)
   - `controller/` - REST endpoints

2. Extend `BaseEntity` for automatic audit fields and soft-delete support

3. Define entity sequence in Oracle:
   ```java
   @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "domain_seq")
   @SequenceGenerator(name = "domain_seq", sequenceName = "DOMAIN_SEQ", allocationSize = 1)
   ```

4. Add `@SQLRestriction("is_deleted = 0")` for soft-delete entities

### Working with Async Operations

- Use `@Async` annotation for long-running tasks
- Configure executor in `SchedulingConfig` (avoid creating ad-hoc thread pools)
- Agent polling tasks automatically use `agentPollingExecutor`
- Return `CompletableFuture<T>` for async methods that need results

### JWT Token Management

- Access tokens include claims: `userId`, `accountId`, `role`, `type` (access/refresh)
- Always validate token type in endpoints (access vs refresh)
- Refresh token endpoint: `/api/auth/refresh` (requires refresh token)
- Token validation extracts user details and sets `SecurityContextHolder`

### WebSocket Integration

To send alerts to a specific user:
```java
@Autowired
private AlertWebSocketHandler webSocketHandler;

AlertMessageDTO message = new AlertMessageDTO(type, title, message, timestamp, alertId);
String json = objectMapper.writeValueAsString(message);
webSocketHandler.sendAlertToUser(userId, json);
```

### Testing External Agent APIs

Agent endpoints are expected to return container metrics in this format:
```json
[
  {
    "containerId": "abc123",
    "containerName": "my-container",
    "status": "running",
    "cpuUsageNanoCores": 250000000,
    "memoryUsageBytes": 536870912,
    "networkRxBytes": 1048576,
    "networkTxBytes": 524288,
    ...
  }
]
```

Use `WebClient` for agent communication (configured in `WebClientConfig`).

## Common Pitfalls

### Agent Polling Issues
- **Problem:** Agents marked OFFLINE unexpectedly
- **Solution:** Check `AgentHealthTracker` failure count; verify agent API is reachable and returns valid JSON

### WebSocket Connection Failures
- **Problem:** Alerts not received on frontend
- **Solution:** Ensure `userId` query parameter is present in WebSocket URL; check `AlertWebSocketHandler` session map

### JWT Token Errors
- **Problem:** 401 Unauthorized despite valid token
- **Solution:** Verify token type (access vs refresh); check `JWT_SECRET` environment variable; ensure token not expired

### Lazy Loading Exceptions
- **Problem:** `LazyInitializationException` when accessing entity relationships
- **Solution:** Use `@Transactional` on service methods or fetch eagerly with `JOIN FETCH` in repository queries

### Soft Delete Confusion
- **Problem:** Entities still appear after deletion
- **Solution:** Ensure `isDeleted` flag is set to `1` (not physically deleted); queries automatically filter via `@SQLRestriction`

## Git Workflow

- **Main branch:** `dev` (use for pull requests)
- **Current branch:** `SCRUM-97-소켓-설계` (WebSocket implementation)
- Branch naming convention: `SCRUM-{ticket-number}-{description}`

## Additional Resources

- **Spring Boot Docs:** https://docs.spring.io/spring-boot/docs/3.4.10/reference/html/
- **Spring Security JWT:** https://github.com/jwtk/jjwt
- **WebSocket Guide:** https://spring.io/guides/gs/messaging-stomp-websocket/