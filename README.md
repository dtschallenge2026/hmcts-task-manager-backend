# HMCTS Task Manager - Backend API

REST API for the HMCTS DTS developer challenge. Built with Java 21 and Spring Boot, backed by PostgreSQL.

> The companion frontend lives in a separate repository.

## Stack

I've used the HMCTS DTS team's own stack (Java/Spring Boot, PostgreSQL, Gradle) rather than my day-to-day stack (C#/.NET), so the submission is directly relevant to the environment the role works in.

| Concern | Choice |
|---|---|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 3.5 |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| API docs | springdoc-openapi (Swagger UI) |
| Build | Gradle (`uk.gov.hmcts.java` plugin) |
| Boilerplate | Lombok |
| Containers | Docker + Docker Compose |

## Architecture

Standard layered Spring Boot app: `TaskController` handles HTTP, delegates to `TaskService` for business logic, which calls `TaskRepository` (Spring Data JPA) against PostgreSQL.

- `Task` entity - `id`, `title` (required), `description` (optional), `status` (enum), `dueDateTime`, `deletedAt`
- `TaskStatus` enum - `TODO`, `IN_PROGRESS`, `DONE`
- Soft delete - `DELETE` sets `deletedAt` rather than removing the row; all queries filter on `deletedAt IS NULL`
- `GlobalExceptionHandler` - returns `404` for missing tasks, `400` for validation failures, warning logs for both
- Actuator health at `/health` with a readiness group that includes the DB probe

## Prerequisites

- Docker (Docker Desktop or Engine + Compose) for the one-command run
- For local dev without Docker: JDK 21 and a PostgreSQL instance. The Gradle wrapper (`./gradlew`) means no separate Gradle install is needed.

## Running with Docker (recommended)

```bash
docker compose up --build
```

Starts two containers:

- `db` - PostgreSQL 16, healthcheck-gated, data in a named volume, exposed on host port 5433
- `backend` - waits for the DB to be healthy, then runs Flyway migrations on startup

API available at http://localhost:4000.

```bash
docker compose down        # stop and remove containers
docker compose down -v     # also wipe the database volume
```

## Running locally (without Docker)

Start just the database:

```bash
docker compose up db
```

Then run the app:

```bash
./gradlew bootRun          # macOS / Linux
.\gradlew.bat bootRun      # Windows
```

App listens on http://localhost:4000, defaulting to Postgres on `localhost:5433`.

### Environment variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | Database host |
| `DB_PORT` | `5433` | Database port |
| `DB_NAME` | `taskmanager` | Database name |
| `DB_USER_NAME` | `taskuser` | Database user |
| `DB_PASSWORD` | `taskpassword` | Database password |
| `DB_OPTIONS` | _(empty)_ | Extra JDBC URL options |

The Docker Compose backend service overrides `DB_HOST=db` and `DB_PORT=5432` to reach the database container.

## Tests

```bash
./gradlew test           # unit tests
./gradlew check          # unit tests, Checkstyle and verification tasks
```

- `TaskServiceTest` - service layer unit tests using Mockito, covering create, list, get-by-id, status update and soft delete (including not-found paths)

JaCoCo coverage reports are written to `build/reports/jacoco/`.

## API reference

Base URL: `http://localhost:4000`

| Method | Path | Description | Status |
|---|---|---|---|
| `POST` | `/tasks` | Create a task | `201 Created` |
| `GET` | `/tasks` | List all active tasks | `200 OK` |
| `GET` | `/tasks/{id}` | Get a task by ID | `200 OK` |
| `PATCH` | `/tasks/{id}/status` | Update a task's status | `200 OK` |
| `DELETE` | `/tasks/{id}` | Soft-delete a task | `204 No Content` |

Task status values: `TODO`, `IN_PROGRESS`, `DONE`

Swagger UI (interactive docs): http://localhost:4000/swagger-ui/index.html

### Examples

Create a task:

```bash
curl -X POST http://localhost:4000/tasks \
  -H "Content-Type: application/json" \
  -d '{
        "title": "Review case file 460825",
        "description": "Initial review before hearing",
        "status": "TODO",
        "dueDateTime": "2026-06-05T09:00:00"
      }'
```

Update status:

```bash
curl -X PATCH http://localhost:4000/tasks/1/status \
  -H "Content-Type: application/json" \
  -d '"IN_PROGRESS"'
```

Delete:

```bash
curl -X DELETE http://localhost:4000/tasks/1
```

### Validation

- `title` - required, must not be blank
- `status` - required, must be a valid enum value
- `dueDateTime` - required, ISO-8601 format

Validation errors return `400`, missing tasks return `404`. Both are logged at warning level.

## Security

- All DB access goes through JPA with parameterised queries - no string-concatenated SQL
- Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`) on all inbound request bodies
- No credentials in source - configuration is environment-driven, with a `configtree` import point for mounted secrets (`/mnt/secrets/test/`) matching HMCTS's deployment pattern
- `ddl-auto: validate` + Flyway - the app can't silently mutate the schema at runtime
- OWASP dependency-check and Checkstyle wired in via the `uk.gov.hmcts.java` Gradle plugin
- Multi-stage Docker image, runs as non-root user

## What I'd add with more time

- OAuth2/OIDC auth (HMCTS IDAM) to protect the endpoints
- Redis caching for read-heavy task lists
- Jenkins pipeline for CI - build, test and scan on every push
- Dynatrace / structured log shipping for production observability
- Terraform + Kubernetes deployment, using the readiness probe already in place
