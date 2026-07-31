-- The fields a person record needs before the reference lists are worth having.
--
-- Each holds a `code` from reference_data rather than a label, so renaming
-- "Did not attend" does not rewrite anybody's record. No foreign key: the codes
-- are validated in the service, and a constraint here would fix the vocabulary
-- to whatever the migration seeded, which is the opposite of letting a clinic
-- add its own.
ALTER TABLE persons
    ADD COLUMN gender                 varchar(40) DEFAULT NULL AFTER last_name,
    ADD COLUMN marital_status         varchar(40) DEFAULT NULL AFTER gender,
    ADD COLUMN blood_type             varchar(40) DEFAULT NULL AFTER marital_status,
    ADD COLUMN identity_document_type varchar(40) DEFAULT NULL AFTER blood_type;

-- A social security number is a thing Indonesia does not have. What goes in this
-- column is a KTP, a passport or a KITAS number, and identity_document_type now
-- says which, so the column is named for what it holds.
ALTER TABLE persons
    CHANGE COLUMN social_security_number identity_number varchar(255) DEFAULT NULL;
