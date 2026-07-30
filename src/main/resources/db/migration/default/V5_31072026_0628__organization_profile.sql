-- The organization registration form. It lives on the tenant row because an
-- organization and its tenant are the same thing: registering one provisions the
-- other.
ALTER TABLE tenants
    ADD COLUMN business_email      varchar(255) DEFAULT NULL AFTER display_name,
    ADD COLUMN photo_key           varchar(512) DEFAULT NULL AFTER business_email,
    ADD COLUMN contact_first_name  varchar(100) DEFAULT NULL AFTER photo_key,
    ADD COLUMN contact_last_name   varchar(100) DEFAULT NULL AFTER contact_first_name,
    ADD COLUMN job_title           varchar(100) DEFAULT NULL AFTER contact_last_name,
    ADD COLUMN phone_number        varchar(50)  DEFAULT NULL AFTER job_title,
    ADD COLUMN org_structure       varchar(40)  DEFAULT NULL AFTER phone_number,
    ADD COLUMN practice_speciality varchar(40)  DEFAULT NULL AFTER org_structure;
