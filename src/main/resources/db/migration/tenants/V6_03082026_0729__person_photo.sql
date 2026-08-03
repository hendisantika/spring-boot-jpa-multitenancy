-- A photo for a person, stored the same way every other upload is: the key
-- here, the object in the bucket, and a signed URL built when it is read.
--
-- Nullable because most records will never have one, and a person without a
-- photo has to render as something other than a broken image.
ALTER TABLE persons
    ADD COLUMN photo_key varchar(255) DEFAULT NULL AFTER identity_number;
