# MeethybridHub Project Documentation

## Project Overview

MeethybridHub is a **production-ready multi-tenant e-commerce SaaS platform** designed with a fashion-first, catalog-agnostic approach. The platform enables businesses to create branded storefronts with shared customer accounts and first-class installment payment support.

### Core Architecture
- **Multi-tenant Architecture**: Shared PostgreSQL schema with `store_id` scoping
- **Hybrid Checkout Model**: Guest checkout for full payments, account required for installment payments
- **Host-header Resolution**: Wildcard DNS for store subdomains (e.g., `divinesignature.meethybridhub.com`)
- **Stateless API**: JWT-based authentication with horizontal scaling capability

---

## Technical Stack

### Backend
| Layer | Technology | Version | Purpose |
|-------|------------|---------|---------|
| Language | Java | 21 | Virtual threads, records, pattern matching |
| Framework | Spring Boot | 3.5.16 | Rapid development framework |
| Security | Spring Security | 3.5.x | Authentication & authorization |
| Persistence | Spring Data JPA + Hibernate | 6.6.x | Object-relational mapping |
| Database | PostgreSQL | 16 | Primary data store |
| Migrations | Flyway | 10+ | Schema version control |
| API Documentation | SpringDoc OpenAPI | 2.8.17 | Swagger UI integration |
| Build Tool | Maven Wrapper | 3.9.11 | Consistent build environment |

### Planned Integrations
- **Authentication**: JWT (Phase 2)
- **Payments**: Korapay (Phase 5)
- **Media Storage**: Cloudinary (Phase 6)
- **Caching**: Redis (Phase 9)
- **Queues**: Message broker (Phase 9)

### Development & Operations
- **CI/CD**: GitHub Actions
- **Testing**: JUnit 5, MockMvc, H2 (Testcontainers for PostgreSQL-specific features)
- **Monitoring**: Spring Boot Actuator
- **Containerization**: Docker (Phase 10)

---

## Codebase Structure & Organization

### Project Layout
```
meethybridhub/
├── src/main/java/com/meethybridhub/
│   ├── MeethybridHubApplication.java          # Application entry point
│   ├── config/                                # Configuration classes
│   │   ├── SecurityConfig.java                # Security configuration
│   │   └── OpenApiConfig.java                 # OpenAPI/Swagger configuration
│   ├── common/                                # Shared utilities
│   │   ├── api/                               # API-related common code
│   │   │   ├── ApiError.java                  # Uniform error envelope
│   │   │   └── GlobalExceptionHandler.java    # Global exception handler
│   │   └── exception/                         # Domain exceptions
│   │       ├── BadRequestException.java       # HTTP 400 exceptions
│   │       └── ResourceNotFoundException.java # HTTP 404 exceptions
│   └── api/                                   # Feature-based API packages
│       └── ping/                              # Ping feature
│           └── PingController.java            # Smoke test endpoint
├── src/main/resources/
│   ├── application.yml                        # Main configuration
│   ├── application-test.yml                   # Test profile configuration
│   └── db/migration/                          # Flyway migrations
├── src/test/java/com/meethybridhub/          # Test classes
├── scripts/                                  # Development scripts
│   └── db.sh                                 # PostgreSQL management script
├── .github/workflows/                        # CI/CD pipelines
│   └── ci.yml                                # GitHub Actions workflow
├── .mvn/wrapper/                             # Maven wrapper configuration
├── pom.xml                                   # Maven dependencies
└── README.md                                 # Project overview
```

### Package Organization Philosophy
The project uses **"package by feature"** rather than traditional "package by layer":
- `api.ping` (current) - will expand to `api.identity`, `api.store`, `api.order`, etc.
- Keeps all related code for a feature together (controllers, services, repositories, entities)
- Improves maintainability and reduces cognitive load

---

## Key Design Decisions & Patterns

### 1. Multi-tenancy Strategy
**Decision**: Shared PostgreSQL schema with `store_id` column on tenant-scoped tables
- **Why**: Simpler management than separate databases/schemas
- **Implementation**: Composite indexes with `store_id` as leading column
- **Isolation**: Row-level filtering through application logic

### 2. Authentication & Authorization
**Decision**: Stateless JWT with role-based access control
- **Why**: Enables horizontal scaling (no session stickiness required)
- **Implementation**: `SessionCreationPolicy.STATELESS` configured from Phase 1
- **Security**: CSRF disabled (no cookies in token-based API)

### 3. Database Schema Management
**Decision**: Flyway migrations as source of truth (not Hibernate `ddl-auto`)
- **Why**: Version-controlled, reproducible schema changes
- **Production**: `ddl-auto: validate` (safety net)
- **Development**: Empty migrations directory until database design phase

### 4. Error Handling
**Decision**: Uniform error envelope (`ApiError` record)
- **Why**: Consistent API contract for all consumers
- **Components**: 
  - `GlobalExceptionHandler` with specific exception mappings
  - Domain-specific exceptions (`BadRequestException`, `ResourceNotFoundException`)
  - Proper HTTP semantics (404 for unknown URLs, not 500)

### 5. API Design
**Decision**: RESTful with OpenAPI documentation
- **Versioning**: `/api/v1/` prefix
- **Documentation**: Live Swagger UI at `/swagger-ui.html`
- **Response Format**: JSON with standardized structure

### 6. Testing Strategy
**Decision**: Layered testing approach
- **Unit Tests**: Business logic in isolation
- **Integration Tests**: `@SpringBootTest` with MockMvc
- **Database Tests**: H2 for general tests, Testcontainers for PostgreSQL-specific features
- **CI**: GitHub Actions with automated test execution

---

## Development Setup & Deployment

### Prerequisites
- **JDK 21** (Temurin recommended: `sdk install java 21.0.5-tem`)
- **PostgreSQL 16** (or Docker for containerized setup)

### Quick Start

#### Option 1: Docker PostgreSQL
```bash
# 1. Start PostgreSQL container
docker run -d --name meethybridhub-db \
  -e POSTGRES_DB=meethybridhub -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:16

# 2. Run the application
./mvnw spring-boot:run

# 3. Access endpoints
# Swagger UI: http://localhost:8080/swagger-ui.html
# Health check: http://localhost:8080/actuator/health
# Ping endpoint: http://localhost:8080/api/v1/ping
```

#### Option 2: Local PostgreSQL (Rootless)
For development machines without root access:
```bash
# Manage PostgreSQL cluster
./scripts/db.sh start    # Initialize and start cluster
./scripts/db.sh status   # Check cluster status
./scripts/db.sh psql     # Interactive psql session
./scripts/db.sh stop     # Stop cluster
```

### Configuration
Environment variables (with defaults in `application.yml`):
```bash
# Database connection
DB_URL=jdbc:postgresql://localhost:5432/meethybridhub
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Server port
PORT=8080
```

### Running Tests
```bash
# Run all tests (uses H2 in-memory database)
./mvnw test

# Run specific test class
./mvnw test -Dtest=ApiSmokeTest

# Run with coverage report
./mvnw clean verify
```

### Build & Package
```bash
# Clean build
./mvnw clean package

# Create executable JAR
./mvnw clean spring-boot:repackage

# Run the packaged JAR
java -jar target/meethybridhub-0.1.0-SNAPSHOT.jar
```

### Continuous Integration
The project includes GitHub Actions workflow (`.github/workflows/ci.yml`):
- Runs on every push to `main` and pull requests
- Builds with JDK 21 Temurin
- Executes all tests
- Uses Maven Wrapper for consistent build environment

---

## Roadmap & Future Phases

### Phase 1: Foundations ✅
- **Status**: COMPLETED
- **Components**: Basic Spring Boot setup, security skeleton, error handling, OpenAPI docs, smoke tests
- **Files**: All core infrastructure files

### Phase 2: Identity 🔄
- **Focus**: JWT authentication, user management, roles, email verification
- **Components**: 
  - User entity and repository
  - Authentication controller with login/register endpoints
  - JWT filter and security configuration
  - Email service for verification
  - Password reset functionality

### Phase 3: Tenancy + Stores
- **Focus**: Store registration, subdomain resolution, branding
- **Components**:
  - Store entity with tenant isolation
  - Domain/subdomain management
  - Store branding (logo, colors, themes)
  - Host-header resolution middleware

### Phase 4: Catalog
- **Focus**: Product management, categories, inventory, reviews
- **Components**:
  - Product catalog with variants
  - Category hierarchy
  - Inventory management
  - Review and rating system
  - Product search and filtering

### Phase 5: Orders + Payments
- **Focus**: Checkout flow, payment processing, installments
- **Components**:
  - Order management system
  - Korapay payment integration
  - Installment payment logic
  - Webhook handlers for payment notifications
  - Idempotency for payment requests

### Phase 6: Custom Orders
- **Focus**: Request → quote → order workflow for custom products
- **Components**:
  - Custom order request system
  - Quote generation and management
  - Designer/store communication
  - Custom order fulfillment workflow

### Phase 7: Discovery
- **Focus**: Store discovery, recommendations, featured content
- **Components**:
  - Store search and discovery
  - Recommendation engine
  - Featured stores/products
  - User preferences and browsing history

### Phase 8: Admin + Analytics
- **Focus**: Administrative tools, reporting, commission management
- **Components**:
  - Admin dashboard
  - Sales analytics and reporting
  - Commission calculation and payout
  - Dispute resolution system
  - Store performance metrics

### Phase 9: Hardening
- **Focus**: Performance optimization, scalability, observability
- **Components**:
  - Redis caching layer
  - Message queues for async processing
  - Distributed tracing (OpenTelemetry)
  - Application metrics and monitoring
  - Rate limiting and circuit breakers

### Phase 10: Deployment
- **Focus**: Production deployment, infrastructure, operations
- **Components**:
  - Docker containerization
  - Production Flyway migrations
  - Database backup strategy
  - Deployment automation
  - Environment configuration management

---

## Architecture Patterns

### 1. Clean Architecture Influence
- **Domain Layer**: Core business logic and entities
- **Application Layer**: Use cases and business rules
- **Infrastructure Layer**: External concerns (database, APIs, messaging)
- **Interface Layer**: Controllers and API endpoints

### 2. CQRS (Command Query Responsibility Segregation)
- **Planned**: Separate models for commands (mutations) and queries (reads)
- **Benefits**: Optimized read/write paths, scalability

### 3. Event Sourcing (Future Consideration)
- **Potential**: For order and payment state changes
- **Benefits**: Audit trail, temporal queries, complex business logic

### 4. Microservices Readiness
- **Current**: Monolithic with clear domain boundaries
- **Future**: Can be decomposed into services (Identity, Catalog, Orders, Payments)
- **Transition Path**: API Gateway pattern, shared database initially

---

## Security Considerations

### Current Security Posture (Phase 1)
- **Stateless Sessions**: `SessionCreationPolicy.STATELESS`
- **CSRF Disabled**: Not needed for token-based APIs
- **Open Endpoints**: All endpoints temporarily open (will be secured in Phase 2)
- **Health Endpoints**: `/actuator/health` publicly accessible

### Planned Security Enhancements
1. **Authentication**: JWT with secure token storage (HttpOnly cookies for web)
2. **Authorization**: Role-based access control (RBAC)
3. **Input Validation**: Comprehensive validation at API boundaries
4. **SQL Injection Prevention**: Parameterized queries via JPA
5. **XSS Protection**: Content Security Policy headers
6. **Rate Limiting**: API rate limiting for public endpoints
7. **Audit Logging**: Security event logging and monitoring

### Compliance Considerations
- **PCI DSS**: Required for payment processing (Phase 5)
- **GDPR**: User data protection and consent management
- **SOC 2**: Security controls for enterprise customers

---

## Performance & Scalability

### Current Architecture
- **Stateless Design**: Enables horizontal scaling
- **Database**: PostgreSQL with connection pooling (HikariCP)
- **Caching**: None yet (planned in Phase 9)

### Scalability Strategy
1. **Horizontal Scaling**: Add more application instances
2. **Database Scaling**: Read replicas, connection pooling optimization
3. **Caching Layer**: Redis for frequently accessed data
4. **Async Processing**: Message queues for background tasks
5. **CDN Integration**: Static assets and media delivery

### Performance Targets
- **API Response Time**: < 200ms for 95th percentile
- **Database Queries**: < 50ms execution time
- **Concurrent Users**: 10,000+ active sessions
- **Availability**: 99.9% uptime SLA

---

## Monitoring & Observability

### Current Monitoring
- **Spring Boot Actuator**: Health, metrics, info endpoints
- **Structured Logging**: JSON logs with correlation IDs
- **Application Metrics**: Basic JVM and application metrics

### Planned Observability Stack
1. **Metrics Collection**: Prometheus integration
2. **Distributed Tracing**: OpenTelemetry with Jaeger
3. **Log Aggregation**: ELK stack or similar
4. **Alerting**: Alert manager with Slack/email notifications
5. **Dashboarding**: Grafana dashboards for business and technical metrics

### Key Metrics to Monitor
- **Business Metrics**: Orders, revenue, conversion rates
- **Technical Metrics**: API latency, error rates, database performance
- **Infrastructure Metrics**: CPU, memory, disk usage, network I/O
- **Security Metrics**: Failed logins, suspicious activities

---

## Contributing Guidelines

### Code Standards
- **Java Code Style**: Follow Google Java Style Guide
- **Documentation**: Comprehensive JavaDoc for public APIs
- **Testing**: Minimum 80% test coverage for new features
- **Commits**: Conventional commits format
- **PR Process**: Code review required, CI must pass

### Development Workflow
1. **Branch Strategy**: Feature branches from `main`
2. **Code Review**: At least one approval required
3. **Testing**: All tests must pass before merge
4. **Documentation**: Update README and API docs as needed
5. **Deployment**: Automated deployment to staging environment

### Quality Gates
- **Static Analysis**: SonarQube integration
- **Security Scanning**: Dependency vulnerability checks
- **Performance Testing**: Load testing for critical paths
- **Compatibility Testing**: Backward compatibility verification

---

## Support & Maintenance

### Support Channels
- **Documentation**: This document and README.md
- **Issue Tracking**: GitHub Issues for bugs and feature requests
- **Discussion Forum**: GitHub Discussions for community support
- **Emergency Support**: Dedicated channel for production issues

### Maintenance Schedule
- **Weekly**: Security updates and dependency bumps
- **Monthly**: Minor feature releases and bug fixes
- **Quarterly**: Major releases with significant new features
- **Annual**: Architectural review and technology stack assessment

### Upgrade Policy
- **Security Patches**: Immediate deployment
- **Minor Versions**: Within 30 days of release
- **Major Versions**: Within 90 days with migration plan
- **Deprecation**: 6-month notice for breaking changes

---

## Conclusion

MeethybridHub represents a modern, scalable e-commerce SaaS platform built with best practices in mind. The architecture is designed for growth from the ground up, with clear separation of concerns, comprehensive error handling, and a well-defined roadmap for future development.

The project demonstrates:
- **Production Readiness**: Comprehensive error handling, monitoring, and security foundations
- **Scalability**: Stateless design, clear domain boundaries, and performance considerations
- **Maintainability**: Feature-based packaging, comprehensive documentation, and testing strategy
- **Extensibility**: Modular design with clear interfaces for future integrations

As the project progresses through its roadmap phases, it will evolve into a full-featured e-commerce platform capable of serving businesses of all sizes with robust, secure, and scalable solutions.

---

*Last Updated: August 2, 2026*  
*Version: 1.0.0*  
*Author: Meethybrid Engineering Team*