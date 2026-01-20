# Project Structure

## Root Layout

```
├── app/                    # Main Quarkus application
├── tests/                  # Integration tests module
├── docker/                 # Docker infrastructure
├── collection/             # Bruno API collection
├── .kiro/                  # Kiro configuration
└── README.md              # Project documentation
```

## Application Structure (`app/`)

### Standard Maven Layout
```
app/
├── src/main/java/tech/buildrun/    # Main source code
├── src/main/resources/             # Configuration & static resources
├── src/test/java/tech/buildrun/    # Test source code
├── src/test/resources/             # Test resources
├── pom.xml                         # Maven configuration
└── target/                         # Build output
```

### Java Package Organization
```
tech.buildrun/
├── controller/             # REST endpoints
│   ├── dto/               # Request/Response DTOs
│   ├── AdminController
│   ├── AuthController
│   ├── BookingController
│   ├── EventController
│   └── UserController
├── entity/                # JPA entities (Panache)
│   ├── AppEntity
│   ├── BookingEntity
│   ├── EventEntity
│   ├── UserEntity
│   └── *Status enums
├── service/               # Business logic
│   ├── dto/              # Service-level DTOs
│   ├── strategy/         # Strategy pattern implementations
│   └── *Service classes
├── listener/              # SQS message listeners
├── exception/             # Custom exceptions & mappers
│   └── dto/              # Exception response DTOs
```

## Architecture Patterns

### Layered Architecture
- **Controller Layer**: REST endpoints, request/response handling
- **Service Layer**: Business logic, transaction management
- **Entity Layer**: JPA entities using Panache Active Record pattern
- **Exception Layer**: Centralized error handling with JAX-RS mappers

### Key Patterns Used
- **Active Record**: Entities extend `PanacheEntity` for simplified data access
- **Strategy Pattern**: Token generation strategies (`TokenStrategy` implementations)
- **DTO Pattern**: Separate DTOs for controllers and services
- **Exception Mapping**: JAX-RS exception mappers for consistent error responses

## Configuration Structure

### Resources (`src/main/resources/`)
```
├── application.properties          # Main configuration
├── application-dev.properties      # Development overrides
├── import.sql                      # Database seed data
├── publicKey.pem                   # JWT public key (dev only)
└── rsaPrivateKey.pem              # JWT private key (dev only)
```

### Docker Infrastructure (`docker/`)
```
├── docker-compose.yml              # PostgreSQL + LocalStack
├── start_local.sh                  # Local startup script
└── volume/                         # Persistent volumes
```

## API Collection (`collection/`)

Bruno API collection with endpoints for:
- Authentication (`Login User`, `Login Admin`, `Login PayGTW`)
- User management (`Create User`, `List Users`, `Setup Admin User`)
- Event management (`Create Event`, `List Events`, `Event by Id`)
- Booking operations (`Booking`, `Confirm Booking`, `Reject Booking`)
- Seat management (`List Event Seats`)

## Testing Structure

### Integration Tests (`tests/integrity-test/`)
- Separate Maven module for integration testing
- Same package structure as main application
- Uses Quarkus test framework with TestContainers pattern

## Naming Conventions

- **Entities**: `*Entity` suffix (e.g., `UserEntity`, `BookingEntity`)
- **Controllers**: `*Controller` suffix with REST resource naming
- **Services**: `*Service` suffix with business domain naming
- **DTOs**: Descriptive names ending in `Dto` (e.g., `CreateUserDto`, `BookingResponseDto`)
- **Exceptions**: Domain-specific names ending in `Exception`
- **Enums**: Status enums follow `*Status` pattern