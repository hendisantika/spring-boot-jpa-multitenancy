-- Invitations let an owner add someone without ever knowing their password:
-- the recipient sets it themselves when accepting.
CREATE TABLE invitations
(
    id                    bigint       NOT NULL AUTO_INCREMENT,
    version               bigint                DEFAULT NULL,
    tenant_slug           varchar(30)  NOT NULL,
    email                 varchar(255) NOT NULL,
    role                  varchar(20)  NOT NULL,
    -- SHA-256 hex of the token, never the token: a leaked database must not
    -- hand out working invitations.
    token_hash            char(64)     NOT NULL,
    status                varchar(20)  NOT NULL,
    invited_by_account_id bigint                DEFAULT NULL,
    created_at            datetime(6)           DEFAULT NULL,
    expires_at            datetime(6)           DEFAULT NULL,
    accepted_at           datetime(6)           DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invitations_token_hash (token_hash),
    KEY ix_invitations_tenant_status (tenant_slug, status),
    KEY ix_invitations_email (email),
    CONSTRAINT fk_invitations_invited_by FOREIGN KEY (invited_by_account_id) REFERENCES accounts (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
