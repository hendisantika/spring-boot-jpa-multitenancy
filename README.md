# spring-boot-jpa-multitenancy

[![Java CI with Maven](https://github.com/hendisantika/spring-boot-jpa-multitenancy/actions/workflows/maven.yml/badge.svg)](https://github.com/hendisantika/spring-boot-jpa-multitenancy/actions/workflows/maven.yml)

Database-per-tenant multi tenancy with Spring Boot, Spring Data JPA, Hibernate, HikariCP, Flyway and MySQL.

Every tenant gets its **own MySQL database**, created at runtime when an organization is registered and reachable at its
own subdomain. The tenant is selected per HTTP request from the host name, and Hibernate transparently routes the JPA
session to the matching connection pool — the entities, repositories and services stay completely tenant-unaware.

```
Organization "Sehat"  ->  database `sehat`  ->  https://sehat.jvm.my.id
Organization "Sehat2" ->  database `sehat2` ->  https://sehat2.jvm.my.id
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
GET https://sehat.jvm.my.id/person/1
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
| `entity/central/TenantRegistration`       | An organization and its tenant: the form, slug, database name, subdomain, status     |
| `entity/central/Account`                  | Someone who signed up on the parent domain                                          |
| `entity/central/UserTenant`               | Grants an account a role in one tenant                                              |
| `config/TenantSecurity`                   | Reads identity and tenant roles from the validated token; `requireOwner`/`requireMember` |
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

### Registering an organization

An organization **is** a tenant: registering one provisions the other. The form is multipart, so the organization photo
arrives with it:

```bash
curl -X POST http://localhost:8080/api/organizations \
  -H "Authorization: Bearer $TOKEN" \
  -F 'organization={
        "businessName":"Sehat",
        "businessEmail":"clinic@sehat.example",
        "contactFirstName":"Hendi","contactLastName":"Santika",
        "jobTitle":"Practice Manager","phoneNumber":"+62 812 3456 7890",
        "orgStructure":"MULTI_LOCATION_CLINIC",
        "practiceSpeciality":"AESTHETIC_AND_DERMA"
      };type=application/json' \
  -F 'photo=@logo.png;type=image/png'
```

```json
{ "slug": "sehat", "businessName": "Sehat", "databaseName": "sehat", "subdomain": "sehat.jvm.my.id", "status": "ACTIVE" }
```

That single call slugifies the business name, validates it, runs `CREATE DATABASE`, applies `db/migration/tenants`,
stores the row in `tenants`, grants the caller an `OWNER` membership and opens the pool — the new tenant serves traffic
**without a restart**.

`orgStructure` is one of `SINGLE_LOCATION_CLINIC`, `MULTI_LOCATION_CLINIC`, `SINGLE_LOCATION_HOSPITAL`,
`MULTI_LOCATION_HOSPITAL`. `practiceSpeciality` is one of `GENERAL_PRACTICE`, `SPECIALIST_PRACTICE`,
`MULTIPLE_PRACTICES_MEDICAL_GROUP`, `HOSPITAL`, `DENTAL`, `AESTHETIC_AND_DERMA`, `ALLIED_HEALTH`, `MENTAL_HEALTH`,
`OTHERS`.

### The owner adds people

```bash
curl -X POST http://localhost:8080/api/organizations/sehat/users \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"email":"nurse@sehat.example","phoneNumber":"+62 813 0000 1111","password":"s3cret-password","role":"MEMBER"}'
```

Only an `OWNER` may add or remove people; a `MEMBER` gets `403`. If the email already has an account it is granted
access rather than duplicated, so one person can belong to several organizations with one login. The owner cannot be
removed from their own organization, which would leave nobody able to administer it.

Slug rules (`TenantSlugs`): lowercase letters and digits only, must start with a letter, 3–30 characters, so the same
string is valid both as a MySQL identifier and as a DNS label. Names are rejected when the slug is reserved
(`mysql`, `sys`, `admin`, …), already registered, or when a database of that name **already exists on the server** —
provisioning never adopts a schema it did not create.

Tenants seeded from the previous enum-based setup keep their historical database names:

| Slug       | Database      | Subdomain             |
|------------|---------------|-----------------------|
| `orgtest1` | `db_orgtest1` | `orgtest1.jvm.my.id` |
| `orgtest2` | `db_orgtest2` | `orgtest2.jvm.my.id` |

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
application.tenant.base-domain=jvm.my.id
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
| `GET`  | `/api/organizations` | bearer      | Organizations the caller belongs to               |
| `POST` | `/api/organizations` | bearer      | Register an organization; caller becomes `OWNER`  |
| `GET`  | `/api/organizations/{slug}` | member | One organization                              |
| `GET`  | `/api/organizations/{slug}/users` | member | Its membership list                     |
| `POST` | `/api/organizations/{slug}/users` | **owner** | Add a person to the organization     |
| `DELETE` | `/api/organizations/{slug}/users/{accountId}` | **owner** | Remove a person       |
| `GET`  | `/organization/{id}` | bearer + membership | Organization by id, from the tenant's database |
| `GET`  | `/person/{id}`       | bearer + membership | Person by id, from the tenant's database       |

The tenant comes from the **host name** — the first label under `application.tenant.base-domain`. A request to the apex
domain or to `localhost` carries no tenant and reads the central database.

```bash
# tenant "sehat" -> database `sehat`
curl -u user:<password> 'https://sehat.jvm.my.id/person/1'
```

Wildcard DNS for `*.jvm.my.id` is not usually available on a developer machine, so the **`X-Tenant` header overrides
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
├── controller/      AuthController, OrganizationRegistrationController,
│                    OrganizationController, PersonController, ApiExceptionHandler
├── entity/
│   ├── central/     Account, TenantRegistration, UserTenant, TenantRole,
│   │                OrgStructure, PracticeSpeciality, statuses
│   ├── tenant/      Organization, Person, User
│   └── support/     BaseEntity (shared by both persistence units)
├── repository/
│   ├── central/     AccountRepository, TenantRegistrationRepository, UserTenantRepository
│   └── tenant/      OrganizationRepository, PersonRepository
├── service/         AuthService, TokenService, MembershipService, TenantProvisioningService,
│   │                TenantSlugs, OrganizationService, PersonService
│   └── storage/     StorageService, S3StorageService
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

`S3StorageIntegrationTest` uploads to a real S3 compatible server, reads the bytes back and deletes them, so the
endpoint, signing and path style settings are exercised rather than mocked. Start MinIO to run it:

```bash
docker run -d --name minio -p 9000:9000 \
  -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio:latest server /data
```

Without it the test **skips**, so no one needs MinIO running to work on the rest. CI sets `S3_INTEGRATION_REQUIRED=true`,
which turns "nothing is listening" into a failure — otherwise a MinIO that failed to start would look like a pass.

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
* `*.jvm.my.id` needs wildcard DNS and a wildcard TLS certificate in production; use the `X-Tenant` header locally.
* **`application.jwt.secret` ships with a development value**; see [Running in production](#running-in-production).
  Anyone holding it can mint tokens for any account.
* Photo uploads go to any S3 compatible endpoint. The defaults point at a local MinIO
  (`http://localhost:9000`, `minioadmin`); set `application.storage.*` for AWS S3, leaving `endpoint` empty to use the
  default credential chain. Uploads are capped at 5 MB and limited to JPEG, PNG and WebP, and the stored key is
  generated rather than taken from the submitted file name.
* **The bucket is not created for you.** `application.storage.bucket` must already exist; only the tests create it.
* Sample users in `Query.sql` have plain-text passwords — sample data only, not for production.

## Running in production

The signing secret in `application.properties` is a development placeholder committed to this repository, so it is not
a secret at all. The `prod` profile takes it from the environment instead, with **no fallback**:

```bash
export APPLICATION_JWT_SECRET=$(openssl rand -base64 48)
java -jar target/multitenancy-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Under `prod`, `production` or `staging`, startup is refused rather than allowed to sign tokens with a public key:

| Situation                                        | Result                                                       |
|--------------------------------------------------|--------------------------------------------------------------|
| `APPLICATION_JWT_SECRET` not set                  | refused — "still the unresolved placeholder ${...}"           |
| Set to the development value from this repository | refused — "the production profile is active but ..."          |
| Shorter than 32 bytes                             | refused — HS256 needs 256 bits                                |
| A real, private value                             | starts                                                        |

Outside those profiles the development value still works, so local runs and tests need no setup — but a warning is
logged every time it is used, in any environment.

Rotating the secret invalidates every token already issued: holders have to log in again.

Database and storage credentials are read from the environment under `prod` too
(`APPLICATION_DATABASE_USER`, `APPLICATION_DATABASE_PASSWORD`, `APPLICATION_STORAGE_ACCESS_KEY`,
`APPLICATION_STORAGE_SECRET_KEY`), though those still fall back to their development values rather than refusing to
start.

## Roadmap

**Phase 1** — tenants are rows, databases are provisioned at runtime, pools open lazily, routing follows the
subdomain. Done.

**Phase 2** — owner signup with photo upload, parent login issuing JWTs, provisioning tied to the authenticated owner.
Done. Membership authorisation was pulled forward from phase 3, because a token that opened every tenant would have
made the rest of the phase decorative.

**Phase 3** — the organization registration form, the owner adding people to it, and roles enforced per endpoint.
Done. Listing is scoped to the caller's memberships, so an account never sees an organization it does not belong to.

The flow from the original brief now runs end to end: sign up → register the organization → a database and a subdomain
appear → the owner adds users → everyone signs in through the parent login and reaches only their own tenants.

Natural next steps, none of them started:

* Invitations instead of the owner setting a member's initial password.
* Password reset and email verification.
* An organization update endpoint; today the form is write-once at registration.
* Per-role rules **inside** a tenant, so `MEMBER` is limited within the business data too, not only in administration.

## Author

Hendi Santika

* Email: hendisantika@gmail.com
* Telegram: [@hendisantika34](https://t.me/hendisantika34)
