# TicketMaster (System Design Interview Project)

A sample event-ticketing backend implemented with Quarkus, Hibernate (Panache), PostgreSQL, AWS SQS integration (via LocalStack for local dev), and JWT-based authentication. This repository is a demo / interview-style project showing a modest microservice with booking, event, user and admin endpoints, background SQS consumers, and JWT authentication.

This README explains how to build, run, test, and develop locally.

Status
- Quarkus platform: 3.26.4
- Java: 21 (project compiled with maven.compiler.release=21)

Contents
- Overview
- Tech stack
- Quick start (dev)
- Configuration
- Running with Docker / LocalStack
- Building a native image
- Tests
- Project structure
- Troubleshooting & notes

## Overview

### The application exposes REST endpoints for managing events, bookings, users and authentication. It uses:
- Hibernate ORM (Panache) for JPA-style persistence
- PostgreSQL as the primary datastore
- AWS SQS for asynchronous background processing (consumer configured via SmallRye SQS)
- SmallRye / MicroProfile JWT for auth

### Tech stack
- Java 21
- Quarkus 3.26.4
- Maven (wrapper included; `mvnw` / `mvnw.cmd`)
- PostgreSQL (local or container)
- LocalStack (recommended for local SQS)

### Prerequisites
- JDK 21 installed and JAVA_HOME configured
- Maven (optional; wrapper is provided so not required)
- Docker (for running LocalStack / Postgres in containers)

### Quick start — development mode

1. Start required local services (Postgres + LocalStack). You can run them via Docker or docker-compose (examples below).
2. Run Quarkus in dev mode (hot reload) from the `app/` directory, with the `local` profile active. The `local` profile is what enables the file-based sample JWT keys and the LocalStack/SQS overrides — without it, the app expects `MP_JWT_VERIFY_PUBLICKEY` / `SMALLRYE_JWT_SIGN_KEY` to be set (see "JWT" below), which only makes sense for the dev/prod ECS environments.

On Linux / macOS

```bash
cd app
QUARKUS_PROFILE=local ./mvnw quarkus:dev
```

On Windows (cmd.exe / PowerShell)

```bat
cd app
set QUARKUS_PROFILE=local
mvnw.cmd quarkus:dev
```

By default the app expects a PostgreSQL instance at jdbc:postgresql://localhost:5432/ticketmasterdb with username/password ticketmaster/ticketmaster (see `app/src/main/resources/application.properties` and `application-local.properties`).

## Configuration (important properties)

See `src/main/resources/application.properties` for the canonical config used in local development. Key values:

- Datasource
  - quarkus.datasource.db-kind=postgresql
  - quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/ticketmasterdb
  - quarkus.datasource.username=ticketmaster
  - quarkus.datasource.password=ticketmaster

- AWS SQS (LocalStack dev)
  - quarkus.sqs.endpoint-override=http://localhost:4566
  - quarkus.sqs.aws.region=sa-east-1
  - quarkus.sqs.aws.credentials.type=static
  - quarkus.sqs.aws.credentials.static-provider.access-key-id=test-key
  - quarkus.sqs.aws.credentials.static-provider.secret-access-key=test-secret
  - queue.check-booking-pending-state.url=http://localhost.localstack.cloud:4566/000000000000/check-booking-pending-state

- JWT
  - mp.jwt.verify.issuer=ticketmaster
  - The actual public/private key content is **not** in the classpath. It comes from:
    - **Local dev** (`local` profile): `mp.jwt.verify.publickey.location=file:./local-keys/publicKey.pem` and `smallrye.jwt.sign.key.location=file:./local-keys/rsaPrivateKey.pem` (see `application-local.properties`), pointing at the sample keys committed under `app/local-keys/`.
    - **Dev/Prod (ECS)**: `mp.jwt.verify.publickey=${MP_JWT_VERIFY_PUBLICKEY}` and `smallrye.jwt.sign.key=${SMALLRYE_JWT_SIGN_KEY}`, where those env vars are injected by the ECS task at boot from an AWS Secrets Manager secret (see `openspec/changes/jwt-keys-secrets-manager/`). No AWS SDK call happens inside the application.

Public endpoints are configured in properties (see `application.properties`):
- `quarkus.http.auth.permission.public.paths=/auth/*,/users,/setup-admin`

Notes on keys and import.sql
- `app/local-keys/publicKey.pem` and `app/local-keys/rsaPrivateKey.pem` are sample RSA keys used **only** when running with `QUARKUS_PROFILE=local` (see "Quick start" above). They are not part of the classpath and are never packaged into the Docker image deployed to dev/prod.
- There is an `import.sql` entry under resources and hibernate is configured to `update` the schema; uncomment `quarkus.hibernate-orm.sql-load-script=import.sql` if you want the SQL seed executed at startup.

Running Postgres + LocalStack with Docker (recommended for local dev)

A minimal docker-compose (example) to run Postgres and LocalStack:

```yaml
version: '3.8'
services:
  db:
    image: postgres:15
    environment:
      POSTGRES_DB: ticketmasterdb
      POSTGRES_USER: ticketmaster
      POSTGRES_PASSWORD: ticketmaster
    ports:
      - "5432:5432"
    volumes:
      - ticketmaster-db-data:/var/lib/postgresql/data

  localstack:
    image: localstack/localstack:1.4
    environment:
      - SERVICES=sqs
      - DEFAULT_REGION=sa-east-1
      - DATA_DIR=/tmp/localstack/data
    ports:
      - "4566:4566"
    volumes:
      - ./localstack:/tmp/localstack

volumes:
  ticketmaster-db-data:
```

Start:

```bash
docker-compose up -d
```

Create an SQS queue (example using AWS CLI configured to point at LocalStack endpoint):

```bash
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name check-booking-pending-state
```

## Building the project

From the project root you can use the Maven wrapper to build a JVM artifact:

On Linux / macOS

```bash
./mvnw package -DskipTests
```

On Windows

```bat
mvnw.cmd package -DskipTests
```

The runnable artifact (Quarkus JVM runner) will be under `target/`.

Running the packaged JVM app

```bash
java -jar target/*-runner.jar
```

## Tests

This project uses JUnit 5 + Quarkus test harness and RestAssured for integration tests. To run unit/integration tests:

```bash
./mvnw test
```

or on Windows

```bat
mvnw.cmd test
```

## Troubleshooting

- DB connection refused: Verify Postgres is running and accessible at the URL in `application.properties`.
- SQS / LocalStack: confirm LocalStack is reachable at `http://localhost:4566` and that the queue `check-booking-pending-state` is created.
- JWT verification errors locally: ensure you're running with `QUARKUS_PROFILE=local` (otherwise the app expects `MP_JWT_VERIFY_PUBLICKEY`/`SMALLRYE_JWT_SIGN_KEY` env vars, not the sample files) and that `app/local-keys/publicKey.pem` matches the private key used for signing tokens.

## Security note

`app/local-keys/` contains sample RSA keys for local development only (used exclusively under the `local` profile). They are never packaged into the build artifact or Docker image. Dev/prod environments source their keys at runtime from AWS Secrets Manager via ECS — see `openspec/changes/jwt-keys-secrets-manager/`.

## Contributing / Next steps

- Add more unit & integration tests covering edge cases.
- Add a docker-compose that wires the app, Postgres and LocalStack for a single-command local dev experience.
- Add API documentation (OpenAPI / Swagger) for endpoint details.

## License

This repository is provided as an example for interview / learning purposes. No license file is included by default.



