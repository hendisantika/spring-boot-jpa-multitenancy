-- When somebody joined an organization. The row already said that they are a
-- member and with what role, but never since when, so a membership screen had
-- nothing to show and no way to work it out.
--
-- Central only: user_tenants is not duplicated per tenant, because one account
-- may belong to several.
--
-- Existing rows stay NULL rather than being backfilled from the account's own
-- creation date. That answers a different question — when the account was
-- registered, not when it joined this organization — and a screen showing it as
-- a joining date would be inventing history.
ALTER TABLE user_tenants
    ADD COLUMN created_at datetime(6) DEFAULT NULL AFTER role;
