-- A requested address, held here rather than on the account, because the change
-- only takes effect once the link sent to it is opened. Until then the account
-- keeps the address it signs in with, so a typo cannot lock anybody out.
CREATE TABLE email_changes
(
    id         bigint       NOT NULL AUTO_INCREMENT,
    version    bigint                DEFAULT NULL,
    account_id bigint       NOT NULL,
    new_email  varchar(255) NOT NULL,
    token_hash char(64)     NOT NULL,
    created_at datetime(6)           DEFAULT NULL,
    expires_at datetime(6)           DEFAULT NULL,
    used_at    datetime(6)           DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_changes_token_hash (token_hash),
    KEY ix_email_changes_account (account_id),
    CONSTRAINT fk_email_changes_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- new_email is deliberately not unique: two people may ask for the same address
-- at once, and requesting it reserves nothing. Whoever confirms first gets it,
-- and the second is refused at that moment, when it is actually taken.
