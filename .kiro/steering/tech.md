# Technology Stack

## Core Technologies

- **Java 21**: Primary programming language (compiled with maven.compiler.release=21)
- **Quarkus 3.26.4**: Supersonic Subatomic Java Framework for cloud-native applications
- **Maven**: Build system with wrapper included (`mvnw`/`mvnw.cmd`)
- **PostgreSQL**: Primary database for persistence
- **LocalStack**: Local AWS services emulation for development

## Key Dependencies

### Web & REST
- `quarkus-rest`: Jakarta REST implementation with Vert.x
- `quarkus-rest-jackson`: JSON serialization support

### Persistence
- `quarkus-hibernate-orm`: JPA implementation
- `quarkus-hibernate-orm-panache`: Simplified JPA with Active Record pattern
- `quarkus-jdbc-postgresql`: PostgreSQL JDBC driver
- `quarkus-hibernate-validator`: Bean validation

### Security
- `quarkus-smallrye-jwt`: JWT authentication and authorization
- `quarkus-smallrye-jwt-build`: JWT token creation
- `quarkus-elytron-security-common`: Security framework

### Messaging & AWS
- `quarkus-amazon-sqs`: AWS SQS integration
- `quarkus-messaging-amazon-sqs`: SmallRye messaging with SQS
- AWS SDK HTTP clients (url-connection-client, netty-nio-client)

### Testing
- `quarkus-junit5`: JUnit 5 integration
- `quarkus-panache-mock`: Mocking support for Panache
- `rest-assured`: REST API testing

## Common Commands

### Development
```bash
# Start in development mode (hot reload)
./mvnw quarkus:dev        # Linux/macOS
mvnw.cmd quarkus:dev      # Windows

# Access Dev UI
http://localhost:8080/q/dev/
```

### Building
```bash
# Package JVM application
./mvnw package -DskipTests

# Build uber-jar
./mvnw package -Dquarkus.package.jar.type=uber-jar

# Run packaged application
java -jar target/*-runner.jar
```

### Native Compilation
```bash
# Build native executable
./mvnw package -Dnative

# Build native in container (no GraalVM required)
./mvnw package -Dnative -Dquarkus.native.container-build=true

# Run native executable
./target/ticketmaster-1.0.0-SNAPSHOT-runner
```

### Testing
```bash
# Run tests
./mvnw test

# Run with integration tests
./mvnw verify
```

### Local Infrastructure
```bash
# Start PostgreSQL + LocalStack
docker-compose up -d

# Create SQS queue (after LocalStack is running)
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name check-booking-pending-state
```

## Configuration Notes

- Database: PostgreSQL on localhost:5432, database `ticketmasterdb`
- SQS: LocalStack endpoint at http://localhost:4566
- JWT: Uses RSA keys in `src/main/resources/` (development only)
- Schema: Hibernate auto-update strategy
- Public endpoints: `/auth/*`, `/users`, `/setup-admin`