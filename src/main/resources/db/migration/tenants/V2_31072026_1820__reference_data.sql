-- The lists every tenant needs on its first day, in its own database.
--
-- These are not the tenant's records; they are the vocabulary its records are
-- written in. They live in one table rather than eight because nothing points
-- at them yet: there is no appointment table to give APPOINTMENT_STATUS a
-- foreign key to. When one arrives, that category is worth promoting to a table
-- of its own so the database can enforce the reference.
--
-- system_defined marks the rows this migration put here. A tenant may add its
-- own alongside them, and a later migration can correct the defaults without
-- touching what the tenant added.
CREATE TABLE reference_data
(
    id             bigint       NOT NULL AUTO_INCREMENT,
    version        bigint                DEFAULT NULL,
    category       varchar(40)  NOT NULL,
    code           varchar(40)  NOT NULL,
    label          varchar(120) NOT NULL,
    sort_order     int          NOT NULL DEFAULT 0,
    active         tinyint(1)   NOT NULL DEFAULT 1,
    system_defined tinyint(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reference_data_category_code (category, code),
    KEY idx_reference_data_category (category, sort_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

INSERT INTO reference_data (category, code, label, sort_order, active, system_defined)
VALUES
-- Indonesian identity documents record two, so intake has to offer what a KTP
-- and a BPJS claim will accept.
('GENDER', 'MALE', 'Male', 1, 1, 1),
('GENDER', 'FEMALE', 'Female', 2, 1, 1),

('MARITAL_STATUS', 'SINGLE', 'Single', 1, 1, 1),
('MARITAL_STATUS', 'MARRIED', 'Married', 2, 1, 1),
('MARITAL_STATUS', 'DIVORCED', 'Divorced', 3, 1, 1),
('MARITAL_STATUS', 'WIDOWED', 'Widowed', 4, 1, 1),

('BLOOD_TYPE', 'A_POSITIVE', 'A+', 1, 1, 1),
('BLOOD_TYPE', 'A_NEGATIVE', 'A-', 2, 1, 1),
('BLOOD_TYPE', 'B_POSITIVE', 'B+', 3, 1, 1),
('BLOOD_TYPE', 'B_NEGATIVE', 'B-', 4, 1, 1),
('BLOOD_TYPE', 'AB_POSITIVE', 'AB+', 5, 1, 1),
('BLOOD_TYPE', 'AB_NEGATIVE', 'AB-', 6, 1, 1),
('BLOOD_TYPE', 'O_POSITIVE', 'O+', 7, 1, 1),
('BLOOD_TYPE', 'O_NEGATIVE', 'O-', 8, 1, 1),

('IDENTITY_DOCUMENT', 'KTP', 'KTP', 1, 1, 1),
('IDENTITY_DOCUMENT', 'KARTU_KELUARGA', 'Kartu Keluarga', 2, 1, 1),
('IDENTITY_DOCUMENT', 'SIM', 'SIM', 3, 1, 1),
('IDENTITY_DOCUMENT', 'PASSPORT', 'Passport', 4, 1, 1),
('IDENTITY_DOCUMENT', 'KITAS', 'KITAS', 5, 1, 1),
('IDENTITY_DOCUMENT', 'BIRTH_CERTIFICATE', 'Birth certificate', 6, 1, 1),

-- Who to call, which every clinic asks for and nobody wants to type freehand.
('RELATIONSHIP', 'SPOUSE', 'Spouse', 1, 1, 1),
('RELATIONSHIP', 'PARENT', 'Parent', 2, 1, 1),
('RELATIONSHIP', 'CHILD', 'Child', 3, 1, 1),
('RELATIONSHIP', 'SIBLING', 'Sibling', 4, 1, 1),
('RELATIONSHIP', 'GUARDIAN', 'Guardian', 5, 1, 1),
('RELATIONSHIP', 'OTHER', 'Other', 6, 1, 1),

('APPOINTMENT_STATUS', 'SCHEDULED', 'Scheduled', 1, 1, 1),
('APPOINTMENT_STATUS', 'CONFIRMED', 'Confirmed', 2, 1, 1),
('APPOINTMENT_STATUS', 'CHECKED_IN', 'Checked in', 3, 1, 1),
('APPOINTMENT_STATUS', 'IN_PROGRESS', 'In progress', 4, 1, 1),
('APPOINTMENT_STATUS', 'COMPLETED', 'Completed', 5, 1, 1),
('APPOINTMENT_STATUS', 'CANCELLED', 'Cancelled', 6, 1, 1),
('APPOINTMENT_STATUS', 'NO_SHOW', 'Did not attend', 7, 1, 1),

('VISIT_TYPE', 'CONSULTATION', 'Consultation', 1, 1, 1),
('VISIT_TYPE', 'FOLLOW_UP', 'Follow-up', 2, 1, 1),
('VISIT_TYPE', 'PROCEDURE', 'Procedure', 3, 1, 1),
('VISIT_TYPE', 'EMERGENCY', 'Emergency', 4, 1, 1),
('VISIT_TYPE', 'TELEMEDICINE', 'Telemedicine', 5, 1, 1),

-- Who settles the bill. BPJS is not optional in an Indonesian clinic.
('PAYER_TYPE', 'SELF_PAY', 'Self-pay', 1, 1, 1),
('PAYER_TYPE', 'BPJS_KESEHATAN', 'BPJS Kesehatan', 2, 1, 1),
('PAYER_TYPE', 'PRIVATE_INSURANCE', 'Private insurance', 3, 1, 1),
('PAYER_TYPE', 'CORPORATE', 'Corporate', 4, 1, 1);
