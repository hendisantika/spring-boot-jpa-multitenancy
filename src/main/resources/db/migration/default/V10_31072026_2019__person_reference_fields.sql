-- The same change as the tenant migration of this name, for the same reason.
--
-- A request that resolves to no tenant falls back to the central database
-- (DynamicRoutingDataSource), so the Person entity is mapped against this
-- persons table as well as every tenant's. Letting the two shapes drift would
-- leave a query that works everywhere except on the fallback.
ALTER TABLE persons
    ADD COLUMN gender                 varchar(40) DEFAULT NULL AFTER last_name,
    ADD COLUMN marital_status         varchar(40) DEFAULT NULL AFTER gender,
    ADD COLUMN blood_type             varchar(40) DEFAULT NULL AFTER marital_status,
    ADD COLUMN identity_document_type varchar(40) DEFAULT NULL AFTER blood_type;

ALTER TABLE persons
    CHANGE COLUMN social_security_number identity_number varchar(255) DEFAULT NULL;
