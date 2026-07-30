-- Accounts live in the central database: the parent login has to authenticate a
-- user and work out which tenants they may reach before any tenant is selected.
CREATE TABLE accounts
(
    id           bigint       NOT NULL AUTO_INCREMENT,
    version      bigint                DEFAULT NULL,
    email        varchar(255) NOT NULL,
    phone_number varchar(50)           DEFAULT NULL,
    password     varchar(255) NOT NULL,
    photo_key    varchar(512)          DEFAULT NULL,
    status       varchar(20)  NOT NULL,
    created_at   datetime(6)           DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_accounts_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- The account that registered the organization owns the tenant.
ALTER TABLE tenants
    ADD COLUMN owner_account_id bigint DEFAULT NULL AFTER display_name,
    ADD CONSTRAINT fk_tenants_owner FOREIGN KEY (owner_account_id) REFERENCES accounts (id);

-- Memberships gain a role and point at the account rather than a bare username.
ALTER TABLE user_tenants
    ADD COLUMN account_id bigint      DEFAULT NULL AFTER version,
    ADD COLUMN role       varchar(20) NOT NULL DEFAULT 'MEMBER' AFTER tenant_slug,
    ADD CONSTRAINT fk_user_tenants_account FOREIGN KEY (account_id) REFERENCES accounts (id);

CREATE INDEX ix_user_tenants_account ON user_tenants (account_id);
CREATE INDEX ix_user_tenants_tenant_slug ON user_tenants (tenant_slug);
