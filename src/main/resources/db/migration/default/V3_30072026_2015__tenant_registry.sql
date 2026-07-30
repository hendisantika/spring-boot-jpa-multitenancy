-- Tenants are rows from here on, not values of a compiled-in enum, so they can be
-- created at runtime when an owner registers an organization.
CREATE TABLE tenants
(
    id            bigint       NOT NULL AUTO_INCREMENT,
    version       bigint                DEFAULT NULL,
    slug          varchar(30)  NOT NULL,
    database_name varchar(64)  NOT NULL,
    subdomain     varchar(255) NOT NULL,
    display_name  varchar(255)          DEFAULT NULL,
    status        varchar(20)  NOT NULL,
    created_at    datetime(6)           DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenants_slug (slug),
    UNIQUE KEY uk_tenants_database_name (database_name),
    UNIQUE KEY uk_tenants_subdomain (subdomain)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- The sample tenants that used to be enum constants. Their databases keep the
-- historical db_ prefix; tenants provisioned from now on use the bare slug.
INSERT INTO tenants (version, slug, database_name, subdomain, display_name, status, created_at)
VALUES (0, 'orgtest1', 'db_orgtest1', 'orgtest1.mhdc.co.id', 'Organization 1', 'ACTIVE', NOW(6)),
       (0, 'orgtest2', 'db_orgtest2', 'orgtest2.mhdc.co.id', 'Organization 2', 'ACTIVE', NOW(6));

-- Memberships now point at a tenant slug rather than an enum ordinal.
ALTER TABLE user_tenants
    ADD COLUMN tenant_slug varchar(30) DEFAULT NULL AFTER version;

UPDATE user_tenants
SET tenant_slug = CASE tenant
                      WHEN 1 THEN 'default'
                      WHEN 2 THEN 'orgtest1'
                      WHEN 3 THEN 'orgtest2'
                      END
WHERE tenant IS NOT NULL;

ALTER TABLE user_tenants
    DROP COLUMN tenant;
