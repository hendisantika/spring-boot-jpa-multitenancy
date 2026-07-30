# spring-boot-jpa-multitenancy

[![Java CI with Maven](https://github.com/hendisantika/spring-boot-jpa-multitenancy/actions/workflows/maven.yml/badge.svg)](https://github.com/hendisantika/spring-boot-jpa-multitenancy/actions/workflows/maven.yml)

Database-per-tenant multi tenancy with Spring Boot, Spring Data JPA, Hibernate, HikariCP, Flyway and MySQL.

Every tenant gets its **own MySQL database**. The tenant is selected per HTTP request, and Hibernate transparently
routes the JPA session to the matching connection pool — the entities, repositories and services stay completely
tenant-unaware.

## Tech stack

| Component        | Version                        |
|------------------|--------------------------------|
| Java             | 21 (toolchain), builds on 21+  |
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
HTTP request ?tenant=orgtest1
        │
        ▼
TenantIdentifierInterceptor      reads the "tenant" request parameter
        │
        ▼
TenantContext (ThreadLocal)      holds the current Tenant for the request
        │
        ├──────────────► TenantIdentifierResolver        (Hibernate CurrentTenantIdentifierResolver)
        │                        │
        │                        ▼
        │                MultitenantConnectionProvider   (Hibernate MultiTenantConnectionProvider)
        │                        │
        └──────────────► RoutingDataSource               (AbstractRoutingDataSource)
                                 │
                 ┌───────────────┼───────────────┐
                 ▼               ▼               ▼
            db_default      db_orgtest1     db_orgtest2
           (Hikari pool)   (Hikari pool)   (Hikari pool)
```

Key classes, all under `src/main/java/id/my/hendisantika/multitenancy`:

| Class                                             | Responsibility                                                                        |
|---------------------------------------------------|---------------------------------------------------------------------------------------|
| `entity/Tenant`                                   | Enum of tenants (`id`, `name`, `description`) + JPA `AttributeConverter`               |
| `config/TenantContext`                            | `ThreadLocal<Tenant>` holder, defaults to `Tenant.DEFAULT`                             |
| `config/TenantIdentifierInterceptor`              | Web interceptor: `?tenant=` → `TenantContext`, cleared in `afterCompletion`             |
| `config/TenantIdentifierResolver`                 | Tells Hibernate which tenant identifier is current                                     |
| `config/RoutingDataSource`                        | Builds one `HikariDataSource` per tenant and routes lookups by `TenantContext`          |
| `config/MultitenantConnectionProvider`            | Hands Hibernate the right `DataSource` for the resolved tenant                          |
| `config/RepositoryConfiguration`                  | `EntityManagerFactory`, transaction manager, Hibernate `MULTI_TENANT=DATABASE` settings |
| `config/FlywayMigrationInitializer`               | Runs Flyway against every tenant database on startup                                    |
| `support/TenantAwareThread`                       | Propagates the tenant to spawned threads (`ThreadLocal` is not inherited)               |

Tenants are declared in the `Tenant` enum and mapped to databases by name using the `db_` prefix:

| Tenant enum | id | Database      |
|-------------|----|---------------|
| `DEFAULT`   | 1  | `db_default`  |
| `ORGTEST1`  | 2  | `db_orgtest1` |
| `ORGTEST2`  | 3  | `db_orgtest2` |

`db_default` additionally holds the `user_tenants` table, which maps a username to the tenant it belongs to.

To add a tenant: add an enum constant to `Tenant`, create the matching `db_<name>` database, and restart — the pool and
the Flyway migration are created automatically.

## Prerequisites

* JDK 21 or newer
* MySQL running on `localhost:3306`
* Maven (or just use the bundled `./mvnw`)

## Setup

### 1. Configure the datasource

`src/main/resources/application.properties` — the literal token `tenantName` in the URL is replaced at runtime with
each tenant's name, so leave it in place:

```properties
application.database.url=jdbc:mysql://localhost:3306/db_tenantName?createDatabaseIfNotExist=true&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Jakarta&useSSL=false&allowPublicKeyRetrieval=true
application.database.user=root
application.database.password=root
```

### 2. (Optional) Load the sample data

No manual database setup is needed — `FlywayMigrationInitializer` migrates every tenant database on startup, and the
JDBC URL carries `createDatabaseIfNotExist=true`, so pointing the app at an empty MySQL is enough.

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

## API

The tenant is chosen with the `tenant` query parameter. An unknown or missing value falls back to `DEFAULT`.

| Method | Endpoint            | Description                      |
|--------|---------------------|----------------------------------|
| `GET`  | `/organization/{id}` | Organization by id, in the tenant DB |
| `GET`  | `/person/{id}`       | Person by id, in the tenant DB       |

Same id, different tenant, different row:

```bash
# default tenant -> db_default
curl -u user:<password> 'http://localhost:8080/organization/1'

# tenant orgtest1 -> db_orgtest1
curl -u user:<password> 'http://localhost:8080/organization/1?tenant=orgtest1'

# tenant orgtest2 -> db_orgtest2
curl -u user:<password> 'http://localhost:8080/person/1?tenant=orgtest2'
```

## Project structure

```
src/main/java/id/my/hendisantika/multitenancy
├── SpringBootJpaMultitenancyApplication.java
├── config/          multi tenancy plumbing (routing, Hibernate, Flyway)
├── controller/      OrganizationController, PersonController
├── entity/          BaseEntity, Organization, Person, User, UserTenant, Tenant
├── repository/      OrganizationRepository, PersonRepository
├── service/         OrganizationService, PersonService
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

`SpringBootJpaMultitenancyApplicationTests` boots the full application context, which opens a connection pool per
tenant, so **a reachable MySQL is required** — the same one configured in `application.properties`. The GitHub Actions
workflow starts a `mysql:8` service container for exactly this reason.

## Database migrations

Migration files are named `Vx_DDMMYYYY_HHMM__description.sql`:

```
V1_30072026_1936__init_schema.sql
V2_30072026_1937__query.sql
```

Flyway treats `_` as a version separator, so `V2_30072026_1937` is version `2.30072026.1937` — the leading `Vx` keeps
the ordering explicit and the timestamp records when the migration was written. The `__` before the description and the
lowercase `.sql` suffix are required by Flyway's default configuration.

## Notes

* Migrations are the source of truth for the schema and run automatically for every tenant on startup, so an empty
  MySQL is enough to boot: Connector/J creates the databases (`createDatabaseIfNotExist=true`) and Flyway creates the
  tables. `Query.sql` is only needed for the sample rows.
* Renaming a migration changes its version, so Flyway will no longer match the history rows of a database that already
  ran the old name — drop the `db_*` databases (or their `flyway_schema_history`) when adopting a new name.
* Editing an already-applied migration changes its checksum and Flyway will refuse to run. During development, drop the
  affected `db_*` database (or its `flyway_schema_history` row) and let it rebuild.
* The tenant is resolved per request into a `ThreadLocal`. Work handed to another thread loses it — wrap it in
  `TenantAwareThread`, or set the tenant on the new thread yourself.
* `TenantContext` falls back to `Tenant.DEFAULT` for an unknown or missing `tenant` parameter, so a typo reads the
  default database rather than failing.
* Sample users in `Query.sql` have plain-text passwords — sample data only, not for production.

## Author

Hendi Santika

* Email: hendisantika@gmail.com
* Telegram: [@hendisantika34](https://t.me/hendisantika34)
