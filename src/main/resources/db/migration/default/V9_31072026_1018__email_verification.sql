CREATE TABLE email_verifications
(
    id         bigint      NOT NULL AUTO_INCREMENT,
    version    bigint               DEFAULT NULL,
    account_id bigint      NOT NULL,
    token_hash char(64)    NOT NULL,
    created_at datetime(6)          DEFAULT NULL,
    expires_at datetime(6)          DEFAULT NULL,
    used_at    datetime(6)          DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_verifications_token_hash (token_hash),
    KEY ix_email_verifications_account (account_id),
    CONSTRAINT fk_email_verifications_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

ALTER TABLE accounts
    ADD COLUMN email_verified_at datetime(6) DEFAULT NULL AFTER password_changed_at;

-- Accounts that predate verification are treated as verified: they were created
-- when nothing asked, and locking them out now would be a surprise.
UPDATE accounts
SET email_verified_at = created_at
WHERE email_verified_at IS NULL;
