# spring-boot-jpa-multitenancy

[![Java CI with Maven](https://github.com/hendisantika/spring-boot-jpa-multitenancy/actions/workflows/maven.yml/badge.svg)](https://github.com/hendisantika/spring-boot-jpa-multitenancy/actions/workflows/maven.yml)

Database-per-tenant multi tenancy with Spring Boot, Spring Data JPA, Hibernate, HikariCP, Flyway and MySQL.

Every tenant gets its **own MySQL database**, created at runtime when an organization is registered and reachable at its
own subdomain. The tenant is selected per HTTP request from the host name, and Hibernate transparently routes the JPA
session to the matching connection pool — the entities, repositories and services stay completely tenant-unaware.

```
Organization "Sehat"  ->  database `sehat`  ->  https://sehat.mhdc.co.id
Organization "Sehat2" ->  database `sehat2` ->  https://sehat2.mhdc.co.id
```

## Tech stack

| Component        | Version                        |
|------------------|--------------------------------|
| Java             | 25                             |
| Spring Boot      | 4.1.0                          |
| Hibernate ORM    | 7.4.1 (via Boot)               |
| Spring Data JPA  | via Boot                       |
| Spring Security  | via Boot                       |
| HikariCP         | 7.1.0                          |
| Flyway           | 12.4.0                         |
| Database         | MySQL 5.7+/8.x                 |
| Build            | Maven (wrapper included)       |

## How it works

```
GET https://sehat.mhdc.co.id/person/1
        │
        ▼
TenantSubdomainInterceptor       first host label -> "sehat"  (X-Tenant header overrides, for dev)
        │
        ▼
TenantContext (ThreadLocal)      holds the tenant slug for this request
        │
        ├──────────────► TenantIdentifierResolver        (Hibernate CurrentTenantIdentifierResolver)
        │                        │
        │                        ▼
        │                MultitenantConnectionProvider   (Hibernate MultiTenantConnectionProvider)
        │                        │
        │                        ▼
        └──────────────► TenantDataSourceRegistry        looks the slug up in `tenants`, opens the
                                 │                       pool on first use
                                 ▼
                         DynamicRoutingDataSource        accepts new tenants without a restart
                                 │
                 ┌───────────────┼───────────────┐
                 ▼               ▼               ▼
             `sehat`         `sehat2`        `klinikx`
           (Hikari pool)   (Hikari pool)   (Hikari pool)
```

### Two persistence units

Tenants are rows, not code, so the registry has to be readable **before** a tenant is known. The application therefore
runs two persistence units, and this separation is the core of the design:

| Persistence unit | Bound to                    | Holds                                                            | Repositories            |
|------------------|-----------------------------|------------------------------------------------------------------|-------------------------|
| `central`        | `db_default` directly       | tenant registry, memberships — later accounts and organizations   | `repository/central/**` |
| `tenant`         | routed per request          | the business data of one organization                             | `repository/tenant/**`  |

Entity packages are disjoint so each unit scans only its own (`entity/central`, `entity/tenant`), with the shared
`BaseEntity` in `entity/support`. `centralEntityManagerFactory` and `centralTransactionManager` are `@Primary`.

Key classes, all under `src/main/java/id/my/hendisantika/multitenancy`:

| Class                                     | Responsibility                                                                     |
|-------------------------------------------|------------------------------------------------------------------------------------|
| `entity/central/TenantRegistration`       | A provisioned tenant: slug, database name, subdomain, status                        |
| `config/TenantContext`                    | `ThreadLocal<String>` holding the current tenant slug, `null` when there is none     |
| `config/TenantSubdomainInterceptor`       | Host name → tenant slug, cleared in `afterCompletion`                               |
| `config/TenantDataSourceRegistry`         | Resolves a slug to a pool, opening it lazily on first use                           |
| `config/DynamicRoutingDataSource`         | `AbstractRoutingDataSource` that accepts tenants registered after startup            |
| `config/MultitenantConnectionProvider`    | Hands Hibernate the right `DataSource` for the resolved tenant                       |
| `config/CentralPersistenceConfiguration`  | Central `EntityManagerFactory`, central Flyway, `@Primary` beans                     |
| `config/TenantPersistenceConfiguration`   | Tenant-aware `EntityManagerFactory` and its transaction manager                      |
| `config/TenantMigrationRunner`            | Brings every registered tenant database up to the latest migration on startup        |
| `service/TenantProvisioningService`       | Creates the database, migrates it, registers and publishes the tenant                |
| `service/TenantSlugs`                     | Slug rules shared by database naming and DNS                                        |
| `support/TenantAwareThread`               | Propagates the tenant to spawned threads (`ThreadLocal` is not inherited)             |

### Provisioning a tenant

```bash
curl -u user:<password> -X POST http://localhost:8080/api/tenants \
  -H 'Content-Type: application/json' \
  -d '{"name":"Sehat"}'
```

```json
{ "slug": "sehat", "databaseName": "sehat", "subdomain": "sehat.mhdc.co.id", "displayName": "Sehat", "status": "ACTIVE" }
```

That single call slugifies the name, validates it, runs `CREATE DATABASE`, applies `db/migration/tenants`, stores the
row in `tenants` and opens the pool — the new tenant serves traffic **without a restart**.

Slug rules (`TenantSlugs`): lowercase letters and digits only, must start with a letter, 3–30 characters, so the same
string is valid both as a MySQL identifier and as a DNS label. Names are rejected when the slug is reserved
(`mysql`, `sys`, `admin`, …), already registered, or when a database of that name **already exists on the server** —
provisioning never adopts a schema it did not create.

Tenants seeded from the previous enum-based setup keep their historical database names:

| Slug       | Database      | Subdomain             |
|------------|---------------|-----------------------|
| `orgtest1` | `db_orgtest1` | `orgtest1.mhdc.co.id` |
| `orgtest2` | `db_orgtest2` | `orgtest2.mhdc.co.id` |

## Prerequisites

* JDK 25
* MySQL running on `localhost:3306`
* Maven (or just use the bundled `./mvnw`)

## Setup

### 1. Configure the datasource

`src/main/resources/application.properties` — `{database}` is substituted per database, so leave the placeholder in
place. The account needs `CREATE`/`DROP DATABASE` privileges, because tenants are provisioned at runtime:

```properties
application.database.url-template=jdbc:mysql://localhost:3306/{database}?createDatabaseIfNotExist=true&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Jakarta&useSSL=false&allowPublicKeyRetrieval=true
application.database.user=root
application.database.password=root
application.database.central-database=db_default
# Every tenant gets its own pool, so keep each one small.
application.database.maximum-pool-size=5
application.tenant.base-domain=mhdc.co.id
spring.flyway.enabled=false
```

`spring.flyway.enabled=false` is deliberate: migrations are driven per database by the application, not by Boot's
single auto-configured Flyway instance.

### 2. (Optional) Load the sample data

No manual database setup is needed — the central database is migrated at startup, every registered tenant database is
brought up to date by `TenantMigrationRunner`, and the JDBC URL carries `createDatabaseIfNotExist=true`, so pointing the
app at an empty MySQL is enough.

To also get the sample organizations / persons / users that the examples below query, load `Query.sql` **after** the
first startup has created the tables (its `CREATE DATABASE` / `CREATE TABLE` statements are redundant now and will
report that the objects already exist; the `INSERT`s are the useful part):

```bash
mysql -u root -p < Query.sql
```

## Run

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. Spring Security is on the classpath, so an HTTP Basic prompt applies; the
generated password is printed to the console at startup.

Build a jar / war instead:

```bash
./mvnw clean package
java -jar target/multitenancy-0.0.1-SNAPSHOT.jar
```

(The application extends `SpringBootServletInitializer`, so it can also be deployed to an external servlet container.)

## Signing up and logging in

Everyone authenticates on the parent domain, whichever tenant they end up using. Signup is multipart so the profile
photo arrives with the rest of the details:

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -F 'account={"email":"owner@example.com","phoneNumber":"+62 812 3456 7890","password":"s3cret-password"};type=application/json' \
  -F 'photo=@me.jpg;type=image/jpeg'

curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"owner@example.com","password":"s3cret-password"}'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "memberships": { "sehat": "OWNER" }
}
```

The access token carries the tenants the account may reach, so a tenant request is authorised without a database round
trip. Registering an organization makes the caller its `OWNER`; that membership appears in the **next** token, so log in
again (or call `/api/auth/refresh`) after creating one. Refresh tokens carry no memberships — they are read fresh from
the database on every refresh, so a grant or revocation takes effect then.

## API

| Method | Endpoint             | Auth        | Description                                      |
|--------|----------------------|-------------|--------------------------------------------------|
| `POST` | `/api/auth/signup`   | open        | Register an owner: email, phone, password, photo  |
| `POST` | `/api/auth/login`    | open        | Exchange credentials for a token pair             |
| `POST` | `/api/auth/refresh`  | open        | Exchange a refresh token for a new pair           |
| `GET`  | `/api/auth/me`       | bearer      | The signed-in account                             |
| `GET`  | `/api/tenants`       | bearer      | List registered tenants                           |
| `POST` | `/api/tenants`       | bearer      | Register an organization; caller becomes `OWNER`  |
| `GET`  | `/organization/{id}` | bearer + membership | Organization by id, from the tenant's database |
| `GET`  | `/person/{id}`       | bearer + membership | Person by id, from the tenant's database       |

The tenant comes from the **host name** — the first label under `application.tenant.base-domain`. A request to the apex
domain or to `localhost` carries no tenant and reads the central database.

```bash
# tenant "sehat" -> database `sehat`
curl -u user:<password> 'https://sehat.mhdc.co.id/person/1'
```

Wildcard DNS for `*.mhdc.co.id` is not usually available on a developer machine, so the **`X-Tenant` header overrides
the host**:

```bash
# same routing, without DNS
curl -u user:<password> -H 'X-Tenant: sehat' 'http://localhost:8080/person/1'
```

Requesting a slug that is not registered, or whose tenant is not `ACTIVE`, fails with `UnknownTenantException` rather
than silently falling back to another database. A token whose memberships do not include the resolved tenant is
refused with `403`, so swapping the host name does not widen access.

## Project structure

```
src/main/java/id/my/hendisantika/multitenancy
├── SpringBootJpaMultitenancyApplication.java
├── config/          routing, the two persistence units, subdomain resolution
├── controller/      TenantController, OrganizationController, PersonController
├── entity/
│   ├── central/     TenantRegistration, TenantStatus, UserTenant
│   ├── tenant/      Organization, Person, User
│   └── support/     BaseEntity (shared by both persistence units)
├── repository/
│   ├── central/     TenantRegistrationRepository
│   └── tenant/      OrganizationRepository, PersonRepository
├── service/         TenantProvisioningService, TenantSlugs, OrganizationService, PersonService
└── support/         TenantAwareThread

src/main/resources
├── application.properties
└── db/migration
    ├── default/     Vx_DDMMYYYY_HHMM__*.sql for db_default (adds user_tenants)
    └── tenants/     Vx_DDMMYYYY_HHMM__*.sql for the tenant databases

Query.sql            optional sample data for every tenant
```

## Testing

```bash
./mvnw test
```

`TenantProvisioningServiceTest` provisions a real tenant: it creates a database, migrates it, writes an `Organization`
through the tenant-routed repository and asserts the row did **not** land in the central database, then drops the
database again. So **a reachable MySQL is required** — the same one configured in `application.properties`, with rights
to create and drop databases. The GitHub Actions workflow starts a `mysql:8` service container for exactly this reason.

## Database migrations

Migration files are named `Vx_DDMMYYYY_HHMM__description.sql`:

```
V1_30072026_1936__init_schema.sql
V2_30072026_1937__query.sql
V3_30072026_2015__tenant_registry.sql
```

Flyway treats `_` as a version separator, so `V2_30072026_1937` is version `2.30072026.1937` — the leading `Vx` keeps
the ordering explicit and the timestamp records when the migration was written. The `__` before the description and the
lowercase `.sql` suffix are required by Flyway's default configuration.

## Notes

* Adding a migration under `db/migration/tenants` reaches every tenant on the next startup, via
  `TenantMigrationRunner`. Tenant databases are migrated one by one, so a long migration multiplies by tenant count.
* Migrations are the source of truth for the schema and an empty MySQL is enough to boot: Connector/J creates the
  databases (`createDatabaseIfNotExist=true`) and Flyway creates the tables. `Query.sql` is only sample rows.
* Renaming or editing an applied migration changes its version or checksum and Flyway will refuse to run. During
  development, drop the affected database (or its `flyway_schema_history` row) and let it rebuild.
* The tenant is resolved per request into a `ThreadLocal`. Work handed to another thread loses it — wrap it in
  `TenantAwareThread`, or set the tenant on the new thread yourself.
* **One pool per tenant does not scale indefinitely.** Pools open lazily on first request and idle connections are
  released after `application.database.idle-timeout`, but the ceiling is still roughly
  `tenants × maximum-pool-size` connections, so `max_connections` on the server bounds how many tenants one instance can
  serve.
* Tenant databases are named after the slug alone, per the naming rule, so they share the server's namespace with every
  other schema. Provisioning refuses a name whose database already exists rather than adopting it, but choosing a
  dedicated MySQL instance (or reinstating a prefix) removes the class of collision entirely.
* `*.mhdc.co.id` needs wildcard DNS and a wildcard TLS certificate in production; use the `X-Tenant` header locally.
* **`application.jwt.secret` ships with a development value and must be overridden** (`APPLICATION_JWT_SECRET`).
  Anyone holding it can mint tokens for any account. HS256 needs at least 32 bytes; startup fails if it is shorter.
* Photo uploads go to any S3 compatible endpoint. The defaults point at a local MinIO
  (`http://localhost:9000`, `minioadmin`); set `application.storage.*` for AWS S3, leaving `endpoint` empty to use the
  default credential chain. Uploads are capped at 5 MB and limited to JPEG, PNG and WebP, and the stored key is
  generated rather than taken from the submitted file name.
* Sample users in `Query.sql` have plain-text passwords — sample data only, not for production.

## Roadmap

**Phase 1** — tenants are rows, databases are provisioned at runtime, pools open lazily, routing follows the
subdomain. Done.

**Phase 2** — owner signup with photo upload, parent login issuing JWTs, provisioning tied to the authenticated owner.
Done. Membership authorisation was pulled forward from phase 3, because a token that opened every tenant would have
made the rest of the phase decorative.

**Phase 3** — still to come:

* The full organization form: business name and email, contact first and last name, job title, phone, org structure
  (single/multi location clinic or hospital) and practice speciality.
* Owner creates users inside their organization, with `MEMBER` memberships.
* Roles enforced per endpoint, so a `MEMBER` cannot do what an `OWNER` can.

## Author

Hendi Santika

* Email: hendisantika@gmail.com
* Telegram: [@hendisantika34](https://t.me/hendisantika34)
