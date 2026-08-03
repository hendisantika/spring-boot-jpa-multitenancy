-- The same change as the tenant migration of this name, for the same reason as
-- the last one: a request that resolves to no tenant falls back to the central
-- database, so the Person entity is mapped against this persons table too and
-- the two shapes must not drift.
ALTER TABLE persons
    ADD COLUMN photo_key varchar(255) DEFAULT NULL AFTER identity_number;
