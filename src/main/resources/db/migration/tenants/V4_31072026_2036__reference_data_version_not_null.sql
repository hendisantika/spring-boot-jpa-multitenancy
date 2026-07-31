-- The seed in V2 did not set `version`, so every row it wrote has NULL there.
--
-- BaseEntity maps that column with @Version, and Hibernate refuses to save a
-- detached entity whose version is null: "Detached entity with generated id has
-- an uninitialized version value". So the rows a migration wrote could be read
-- but never edited — which is exactly what a tenant would want to do to one of
-- them, to stop offering a value.
--
-- Backfilled and then made NOT NULL, so a future INSERT that forgets it gets 0
-- rather than repeating this.
UPDATE reference_data
SET version = 0
WHERE version IS NULL;

ALTER TABLE reference_data
    MODIFY COLUMN version bigint NOT NULL DEFAULT 0;
