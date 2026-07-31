# spring-boot-jpa-multitenancy

[![Java CI with Maven](https://github.com/hendisantika/spring-boot-jpa-multitenancy/actions/workflows/maven.yml/badge.svg)](https://github.com/hendisantika/spring-boot-jpa-multitenancy/actions/workflows/maven.yml)
[![Frontend CI](https://github.com/hendisantika/spring-boot-jpa-multitenancy/actions/workflows/frontend.yml/badge.svg)](https://github.com/hendisantika/spring-boot-jpa-multitenancy/actions/workflows/frontend.yml)

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

### Editing an organization

```bash
curl -X PUT http://localhost:8080/api/organizations/sehat \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -F 'organization={…same fields as registration…};type=application/json' \
  -F 'photo=@new-logo.png;type=image/png'
```

Owner only. **The slug, database name and subdomain do not change**, whatever the business name becomes: rows are
routed by the slug, a database cannot be renamed underneath running connections, and the subdomain may already be in
somebody's bookmarks. Renaming changes the label and nothing else.

Omitting the `photo` part keeps the current photo; sending one replaces it and the previous object is deleted, so edits
do not leave orphans in the bucket.

### The owner invites people

```bash
curl -X POST http://localhost:8080/api/organizations/sehat/invitations \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"email":"nurse@sehat.example","role":"MEMBER"}'
```

```json
{ "id": 1, "email": "nurse@sehat.example", "role": "MEMBER", "expiresAt": "…",
  "acceptUrl": "http://localhost:3000/invitations/m9f6YA7gtJNRs6…" }
```

The recipient opens that link and **chooses their own password**, so nobody else, the owner included, ever handles it.
Accepting signs them in, so they land inside the organization rather than at a login form.

| Property | Behaviour |
|---|---|
| Storage | Only a SHA-256 of the token is kept, so a leaked database hands out no working invitations |
| Lifetime | `application.invitation.ttl`, 7 days by default |
| Reuse | Single use; accepted, revoked and expired links all fail the same way |
| Re-inviting | Withdraws the previous link, so two usable links never exist for one person |
| Existing account | Granted the membership and keeps the password it already has |
| Wrong or unknown token | One message for every failure, so it reveals nothing about what exists |

**The link is emailed through Brevo** when `BREVO_API_KEY` is set, and `acceptUrl` is then **not** returned: once the
recipient's mailbox has it, the owner has no reason to hold a credential that would let them accept on that person's
behalf. With no key configured, delivery is off and `acceptUrl` comes back instead for the owner to pass on, which is a
supported way to run this.

```properties
application.brevo.api-key=${BREVO_API_KEY:}
application.brevo.sender-email=${BREVO_SENDER_EMAIL:no-reply@jvm.my.id}
application.brevo.sender-name=${BREVO_SENDER_NAME:Multitenancy}
```

The sender must be an address Brevo has verified for the account, or it refuses the message. A delivery failure is
reported, never thrown: the invitation is already created, and losing it would be worse than an undelivered mail, so
the response falls back to carrying the link.

`GET`/`DELETE /api/organizations/{slug}/invitations` list and revoke the pending ones.

### The owner adds people directly

```bash
curl -X POST http://localhost:8080/api/organizations/sehat/users \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"email":"nurse@sehat.example","phoneNumber":"+62 813 0000 1111","password":"s3cret-password","role":"MEMBER"}'
```

Only an `OWNER` may add, invite or remove people; a `MEMBER` gets `403`. If the email already has an account it is
granted access rather than duplicated, so one person can belong to several organizations with one login. The owner
cannot be removed from their own organization, which would leave nobody able to administer it.

This direct path has the owner set someone's initial password, which invitations exist to avoid. It is kept for
seeding and for staff who cannot receive a link; prefer inviting.

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
* MySQL on `localhost:3306` and an S3 compatible bucket — `docker compose up -d` provides both
* Maven (or just use the bundled `./mvnw`)

## Configuration

Everything in `application.properties` can be overridden by an environment variable: uppercase the property and turn
each dot and dash into an underscore, so `application.jwt.access-token-ttl` becomes `APPLICATION_JWT_ACCESS_TOKEN_TTL`.

`.env.example` lists every variable with its default and what it is for. Copy it and edit:

```bash
cp .env.example .env

docker compose --profile app --env-file .env up -d   # containers
set -a && . ./.env && set +a && ./mvnw spring-boot:run   # from a shell
```

`.env` is git-ignored; `.env.example` is committed, so keep real credentials out of it.

## Dependencies with Docker Compose

`compose.yaml` brings up both dependencies with the same values `application.properties` expects, so the application
needs no extra configuration:

```bash
docker compose up -d
```

| Service      | Port   | Credentials             | Notes                                            |
|--------------|--------|-------------------------|--------------------------------------------------|
| `mysql`      | 3306   | `root` / `root`         | Data survives restarts in the `mysql-data` volume |
| `minio`      | 9000   | `minioadmin` / `minioadmin` | S3 API the application talks to               |
| `minio`      | 9001   | same                    | Web console, <http://localhost:9001>              |
| `minio-init` | —      | —                       | Runs once and creates the `jvm-uploads` bucket    |
| `redis`      | 6379   | —                       | Shared rate limit counters                        |

`minio-init` matters: the application does **not** create the bucket, so without it the first photo upload fails.

**If MySQL is already installed on this machine** it will be holding port 3306 and the `mysql` service cannot start.
Either stop the local server, or map the container to another port and point `application.database.url-template` at it.

```bash
docker compose down          # stop, keep the data
docker compose down -v       # stop and delete the databases and bucket
```

### Running the application in a container too

`Dockerfile` builds the application; the `app` service in `compose.yaml` runs it against the other two. It sits behind
a profile, so the plain `docker compose up -d` above still starts only the dependencies — which is what you want while
running the application from an IDE.

```bash
docker compose --profile app up -d --build   # everything, on http://localhost:8080
docker build -t multitenancy:latest .        # just the image
```

The build is two stages: dependencies resolve before the sources are copied so editing code does not re-download them,
the jar is split into layers so a code change does not invalidate the dependency layer, and the runtime stage is a JRE
running as an unprivileged user. Tests are skipped inside the image — they need a database and a bucket, which a build
does not have; CI is what runs them.

Heap is left to the container limit via `-XX:MaxRAMPercentage=75.0`; override `JAVA_OPTS` to change it.

### Health

Actuator exposes exactly one endpoint, and it needs no token because the container `HEALTHCHECK` and any orchestrator
probe it anonymously:

```bash
curl http://localhost:8080/actuator/health              # {"status":"UP"}
curl http://localhost:8080/actuator/health/liveness     # process is alive
curl http://localhost:8080/actuator/health/readiness    # accepting traffic
```

Details are hidden unless the caller is authorized, so an anonymous probe learns `UP` or `DOWN` and nothing about the
database behind it. Every other actuator endpoint is unexposed.

The container `HEALTHCHECK` deliberately probes `/actuator/health` rather than `/actuator/health/readiness`: the default
readiness group contains only `readinessState` and **stays `UP` with the database unreachable**, while the overall group
includes the database. Confirmed by pulling the network from a running container — the overall status flipped to `DOWN`
and Docker reported the container `unhealthy`, while readiness still claimed `UP`.

For Kubernetes, use `/actuator/health/liveness` and `/actuator/health/readiness` instead: tying readiness to the
database means every pod goes unready together during a database blip.

## Setup

### 1. Configure the datasource

`src/main/resources/application.properties` — `{database}` is substituted per database, so leave the placeholder in
place. The account needs `CREATE`/`DROP DATABASE` privileges, because tenants are provisioned at runtime:

```properties
application.database.url-template=jdbc:mysql://localhost:3306/{database}?createDatabaseIfNotExist=true&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&connectionTimeZone=UTC&useSSL=false&allowPublicKeyRetrieval=true
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

### Email verification

Signing up sends a confirmation link. The account can sign in straight away, but **registering an organization is
refused until the address is confirmed** — provisioning a database on an unproved address is how junk tenants get
created.

| Situation | Behaviour |
|---|---|
| Brevo configured | Signup sends a link; `POST /api/auth/verify-email/{token}` confirms it |
| **No Brevo key** | Accounts are **verified at signup**, since nothing could arrive and requiring proof would lock everyone out |
| Accepting an invitation | Counts as proof: opening the link shows the same thing a confirmation mail asks for |
| Resending | Invalidates the earlier link; refused once already verified |

`application.email-verification.ttl` is 24 hours. `/api/auth/me` reports `emailVerified`, which the dashboard uses to
show a reminder.

### Resetting a password

```bash
curl -X POST http://localhost:8080/api/auth/password/forgot \
  -H 'Content-Type: application/json' -d '{"email":"owner@example.com"}'
```

The answer is the same whether or not the address has an account, so the endpoint cannot be used to find out who is
registered. When it does exist, a link is emailed; with delivery off, `resetUrl` comes back instead.

| Property | Behaviour |
|---|---|
| Storage | Only a SHA-256 of the token is kept |
| Lifetime | `application.password-reset.ttl`, 1 hour by default: shorter than an invitation, since it is a way into an existing account |
| Reuse | Single use, and asking again invalidates the earlier link |
| After a reset | **Refresh tokens issued before it stop working**, so resetting a stolen password does not leave a fortnight of access |

Access tokens are not revoked, since nothing checks them against the database; they expire on their own within
`application.jwt.access-token-ttl`, 30 minutes by default. That window is the trade for stateless authentication.

## API

| Method | Endpoint             | Auth        | Description                                      |
|--------|----------------------|-------------|--------------------------------------------------|
| `POST` | `/api/auth/signup`   | open        | Register an owner: email, phone, password, photo  |
| `POST` | `/api/auth/login`    | open        | Exchange credentials for a token pair             |
| `POST` | `/api/auth/refresh`  | open        | Exchange a refresh token for a new pair           |
| `POST` | `/api/auth/verify-email/{token}` | open | Confirm an email address                    |
| `POST` | `/api/auth/verify-email/resend` | bearer | Send a fresh confirmation link            |
| `POST` | `/api/auth/password/forgot` | open | Ask for a reset link                            |
| `GET`  | `/api/auth/password/reset/{token}` | open | Whose account the link belongs to        |
| `POST` | `/api/auth/password/reset/{token}` | open | Set a new password                       |
| `GET`  | `/api/auth/me`       | bearer      | The signed-in account                             |
| `GET`  | `/api/organizations` | bearer      | Organizations the caller belongs to               |
| `POST` | `/api/organizations` | bearer      | Register an organization; caller becomes `OWNER`  |
| `GET`  | `/api/organizations/{slug}` | member | One organization                              |
| `PUT`  | `/api/organizations/{slug}` | **owner** | Edit the profile; identity stays put       |
| `GET`  | `/api/organizations/{slug}/users` | member | Its membership list                     |
| `POST` | `/api/organizations/{slug}/users` | **owner** | Add a person directly, setting their password |
| `GET`  | `/api/organizations/{slug}/invitations` | **owner** | Pending invitations                |
| `POST` | `/api/organizations/{slug}/invitations` | **owner** | Invite someone; returns the accept link |
| `DELETE` | `/api/organizations/{slug}/invitations/{id}` | **owner** | Withdraw an invitation      |
| `GET`  | `/api/invitations/{token}` | open | What the accept page shows before committing         |
| `POST` | `/api/invitations/{token}/accept` | open | Accept, choosing a password; signs you in     |
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

`RedisRateLimiterIntegrationTest` runs the Lua script against a real Redis, including forty concurrent callers racing
for twenty tokens, because the atomicity it exists for cannot be shown any other way. It skips when Redis is not
answering; CI sets `REDIS_INTEGRATION_REQUIRED=true` so a Redis that failed to start fails the build.

`S3StorageIntegrationTest` uploads to a real S3 compatible server, reads the bytes back and deletes them, so the
endpoint, signing and path style settings are exercised rather than mocked. `docker compose up -d` (see
[Dependencies with Docker Compose](#dependencies-with-docker-compose)) provides it.

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
* **The bucket is not created for you.** `application.storage.bucket` must already exist. Locally the `minio-init`
  service in `compose.yaml` creates it; in production create it as part of provisioning.
* Sample users in `Query.sql` have plain-text passwords — sample data only, not for production.

## Roles inside a tenant's data

Membership decides what you may do with the business data behind the subdomain, not just who may administer the
organization. The rules sit on the methods they guard, as `@PreAuthorize("@tenantSecurity.isOwnerOfCurrentTenant()")`,
and read the tenant from the request rather than from a path.

| Endpoint | `MEMBER` | `OWNER` |
|---|---|---|
| `GET /person`, `GET /person/{id}` | yes | yes |
| `POST /person`, `PUT /person/{id}` | yes | yes |
| `DELETE /person/{id}` | **no** | yes |
| `GET /organization`, `GET /organization/{id}` | yes | yes |
| `POST`, `PUT`, `DELETE /organization/{id}` | **no** | yes |

A member may read and write people, because that is the daily work and withholding it would leave the role useless.
Deleting is owner only: a removed record is not something a shift should undo by mistake. The tenant-scoped
organizations are closer to the shape of the business than to its daily work, so changing them is owner only too.

Roles are **per tenant**: owning one organization buys nothing in another, and a request that resolves to no tenant at
all cannot write, so nothing lands in the central database by accident.

The frontend follows the same table at `/organizations/[slug]/people` and `/organizations/[slug]/units`: a member sees
no `Delete`, and is told plainly that units are the owner's to change. That is courtesy, not security — the API refuses
those calls regardless of what the page offers.

## Rate limiting

Sign-in and forgot-password are open by necessity, so both are limited.

| Endpoint | Default | Counted |
|---|---|---|
| `POST /api/auth/login` | 10 per 5 minutes | **failures only** — a correct password never counts, so nobody is locked out by signing in |
| `POST /api/auth/password/forgot` | 5 per 15 minutes | every request, because each one sends mail |

Each request is keyed by client address **and** by the email in the body. Either alone leaves a hole: by IP only
punishes everyone behind one NAT and lets a botnet through, by email only lets a single host work through a list of
addresses. Refused requests answer `429` with `Retry-After`.

```properties
application.rate-limit.enabled=true
application.rate-limit.login.capacity=10
application.rate-limit.login.window=5m
application.rate-limit.forgot-password.capacity=5
application.rate-limit.forgot-password.window=15m
```

**The counters live in Redis**, so every instance draws from the same allowance rather than each granting its own. The
whole read-modify-write runs inside one Lua script, because doing it in Java would let two instances read the same
count and both decide they are under the limit, and the clock comes from Redis so instances whose clocks disagree
cannot refill each other's buckets.

```properties
# AUTO uses Redis when it answers at startup and counts per instance otherwise.
# Set REDIS in production so a missing Redis is a failure, not a quiet downgrade.
application.rate-limit.backend=AUTO
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

Without Redis the counters fall back to this process, which still slows guessing down but lets each instance grant the
rate separately; keys are capped and refilled ones evicted so that cannot grow memory without bound. Redis expires them
instead.

**If Redis goes down the limiter fails open**, allowing requests rather than locking everyone out of signing in. The
trade is that protection is lost exactly while Redis is unavailable.

## Running in production

Every credential in `application.properties` is a development placeholder committed to this repository, so none of them
are secret. The `prod` profile takes them from the environment instead, with **no fallback**:

```bash
export APPLICATION_JWT_SECRET=$(openssl rand -base64 48)
export APPLICATION_DATABASE_USER=appuser
export APPLICATION_DATABASE_PASSWORD='…'
# Both, or neither to use the AWS default credential chain:
export APPLICATION_STORAGE_ACCESS_KEY='…'
export APPLICATION_STORAGE_SECRET_KEY='…'

java -jar target/multitenancy-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Under `prod`, `production` or `staging`, startup is refused rather than allowed to run on credentials that are public:

| Situation                                          | Result                                              |
|----------------------------------------------------|-----------------------------------------------------|
| A variable is not set                               | refused — "still the unresolved placeholder ${...}"  |
| Set to the development value from this repository   | refused — "the production profile is active but ..." |
| JWT secret shorter than 32 bytes                    | refused — HS256 needs 256 bits                       |
| Storage access key set without its secret key       | refused — supply both or neither                     |
| **Storage keys both empty**                         | **starts** — AWS default credential chain (IAM role) |
| Real, private values                                | starts                                               |

Empty storage keys are deliberately allowed: that is how an instance running with an IAM role is configured, and
demanding a key there would rule out the better practice.

The check runs before the connection pool and the S3 client are built, so a misconfiguration stops with an explanation
rather than a connection error. Outside those profiles the development values still work, so local runs and tests need
no setup — but a warning is logged whenever they are in use, in any environment.

Rotating the JWT secret invalidates every token already issued: holders have to log in again.

Note that the local development database uses `root`/`root`, which the `prod` profile rejects by design. Running the
prod profile against it needs a database user created for the purpose.

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

* Password reset and email verification.

## Author

Hendi Santika

* Email: hendisantika@gmail.com
* Telegram: [@hendisantika34](https://t.me/hendisantika34)
