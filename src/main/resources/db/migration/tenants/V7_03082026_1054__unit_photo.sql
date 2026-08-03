-- A photo for a business unit, stored the way every other upload is: the key
-- here, the object in the bucket, and a signed URL built when it is read.
--
-- Nullable because most units will never have one, and a unit without a photo
-- has to render as something other than a broken image.
ALTER TABLE organizations
    ADD COLUMN photo_key varchar(255) DEFAULT NULL AFTER email;
