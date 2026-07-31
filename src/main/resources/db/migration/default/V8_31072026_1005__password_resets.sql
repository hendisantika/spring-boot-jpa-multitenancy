CREATE TABLE password_resets
(
    id         bigint      NOT NULL AUTO_INCREMENT,
    version    bigint               DEFAULT NULL,
    account_id bigint      NOT NULL,
    -- SHA-256 hex of the token, never the token itself.
    token_hash char(64)    NOT NULL,
    created_at datetime(6)          DEFAULT NULL,
    expires_at datetime(6)          DEFAULT NULL,
    used_at    datetime(6)          DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_resets_token_hash (token_hash),
    KEY ix_password_resets_account (account_id),
    CONSTRAINT fk_password_resets_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Refresh tokens live for two weeks, so a reset has to be able to disown the
-- ones handed out before it. Anything issued earlier than this is refused.
ALTER TABLE accounts
    ADD COLUMN password_changed_at datetime(6) DEFAULT NULL AFTER password;
