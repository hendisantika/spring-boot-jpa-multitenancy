-- Dropdowns for a business unit, which the first catalogue had nothing for.
--
-- GENDER, BLOOD_TYPE, RELATIONSHIP and the rest belong to a person. A unit is a
-- place, and what a place has is a kind, an address and whether it is open, so
-- those are the three lists added here.
--
-- Same shape as V2: a code that survives renames, a sort order a clinic reads
-- in, an active flag, and system_defined marking what a migration put here.
INSERT INTO reference_data (version, category, code, label, sort_order, active, system_defined)
VALUES (0, 'UNIT_TYPE', 'MAIN_CLINIC', 'Main clinic', 1, 1, 1),
       (0, 'UNIT_TYPE', 'BRANCH_CLINIC', 'Branch clinic', 2, 1, 1),
       (0, 'UNIT_TYPE', 'SATELLITE', 'Satellite point', 3, 1, 1),
       (0, 'UNIT_TYPE', 'HOSPITAL', 'Hospital', 4, 1, 1),
       (0, 'UNIT_TYPE', 'LABORATORY', 'Laboratory', 5, 1, 1),
       (0, 'UNIT_TYPE', 'PHARMACY', 'Pharmacy', 6, 1, 1),
       (0, 'UNIT_TYPE', 'IMAGING_CENTRE', 'Imaging centre', 7, 1, 1),
       (0, 'UNIT_TYPE', 'ADMINISTRATIVE_OFFICE', 'Administrative office', 8, 1, 1),

       -- Closed is not deleted: a unit that shut last month still owns its
       -- records, so it needs a state rather than a removal.
       (0, 'OPERATING_STATUS', 'OPEN', 'Open', 1, 1, 1),
       (0, 'OPERATING_STATUS', 'OPENING_SOON', 'Opening soon', 2, 1, 1),
       (0, 'OPERATING_STATUS', 'TEMPORARILY_CLOSED', 'Temporarily closed', 3, 1, 1),
       (0, 'OPERATING_STATUS', 'PERMANENTLY_CLOSED', 'Permanently closed', 4, 1, 1),

       -- The 38 provinces, west to east, which is how an Indonesian address is
       -- read. The address itself stays free text; this is the part worth
       -- filtering and reporting on.
       (0, 'PROVINCE', 'ACEH', 'Aceh', 1, 1, 1),
       (0, 'PROVINCE', 'SUMATERA_UTARA', 'Sumatera Utara', 2, 1, 1),
       (0, 'PROVINCE', 'SUMATERA_BARAT', 'Sumatera Barat', 3, 1, 1),
       (0, 'PROVINCE', 'RIAU', 'Riau', 4, 1, 1),
       (0, 'PROVINCE', 'JAMBI', 'Jambi', 5, 1, 1),
       (0, 'PROVINCE', 'SUMATERA_SELATAN', 'Sumatera Selatan', 6, 1, 1),
       (0, 'PROVINCE', 'BENGKULU', 'Bengkulu', 7, 1, 1),
       (0, 'PROVINCE', 'LAMPUNG', 'Lampung', 8, 1, 1),
       (0, 'PROVINCE', 'BANGKA_BELITUNG', 'Kepulauan Bangka Belitung', 9, 1, 1),
       (0, 'PROVINCE', 'KEPULAUAN_RIAU', 'Kepulauan Riau', 10, 1, 1),
       (0, 'PROVINCE', 'DKI_JAKARTA', 'DKI Jakarta', 11, 1, 1),
       (0, 'PROVINCE', 'JAWA_BARAT', 'Jawa Barat', 12, 1, 1),
       (0, 'PROVINCE', 'JAWA_TENGAH', 'Jawa Tengah', 13, 1, 1),
       (0, 'PROVINCE', 'DI_YOGYAKARTA', 'DI Yogyakarta', 14, 1, 1),
       (0, 'PROVINCE', 'JAWA_TIMUR', 'Jawa Timur', 15, 1, 1),
       (0, 'PROVINCE', 'BANTEN', 'Banten', 16, 1, 1),
       (0, 'PROVINCE', 'BALI', 'Bali', 17, 1, 1),
       (0, 'PROVINCE', 'NUSA_TENGGARA_BARAT', 'Nusa Tenggara Barat', 18, 1, 1),
       (0, 'PROVINCE', 'NUSA_TENGGARA_TIMUR', 'Nusa Tenggara Timur', 19, 1, 1),
       (0, 'PROVINCE', 'KALIMANTAN_BARAT', 'Kalimantan Barat', 20, 1, 1),
       (0, 'PROVINCE', 'KALIMANTAN_TENGAH', 'Kalimantan Tengah', 21, 1, 1),
       (0, 'PROVINCE', 'KALIMANTAN_SELATAN', 'Kalimantan Selatan', 22, 1, 1),
       (0, 'PROVINCE', 'KALIMANTAN_TIMUR', 'Kalimantan Timur', 23, 1, 1),
       (0, 'PROVINCE', 'KALIMANTAN_UTARA', 'Kalimantan Utara', 24, 1, 1),
       (0, 'PROVINCE', 'SULAWESI_UTARA', 'Sulawesi Utara', 25, 1, 1),
       (0, 'PROVINCE', 'SULAWESI_TENGAH', 'Sulawesi Tengah', 26, 1, 1),
       (0, 'PROVINCE', 'SULAWESI_SELATAN', 'Sulawesi Selatan', 27, 1, 1),
       (0, 'PROVINCE', 'SULAWESI_TENGGARA', 'Sulawesi Tenggara', 28, 1, 1),
       (0, 'PROVINCE', 'GORONTALO', 'Gorontalo', 29, 1, 1),
       (0, 'PROVINCE', 'SULAWESI_BARAT', 'Sulawesi Barat', 30, 1, 1),
       (0, 'PROVINCE', 'MALUKU', 'Maluku', 31, 1, 1),
       (0, 'PROVINCE', 'MALUKU_UTARA', 'Maluku Utara', 32, 1, 1),
       (0, 'PROVINCE', 'PAPUA_BARAT', 'Papua Barat', 33, 1, 1),
       (0, 'PROVINCE', 'PAPUA_BARAT_DAYA', 'Papua Barat Daya', 34, 1, 1),
       (0, 'PROVINCE', 'PAPUA', 'Papua', 35, 1, 1),
       (0, 'PROVINCE', 'PAPUA_SELATAN', 'Papua Selatan', 36, 1, 1),
       (0, 'PROVINCE', 'PAPUA_TENGAH', 'Papua Tengah', 37, 1, 1),
       (0, 'PROVINCE', 'PAPUA_PEGUNUNGAN', 'Papua Pegunungan', 38, 1, 1);

-- Codes, never labels, for the same reason as on a person: renaming a label
-- should not rewrite anybody's record.
ALTER TABLE organizations
    ADD COLUMN unit_type        varchar(40) DEFAULT NULL AFTER name,
    ADD COLUMN operating_status varchar(40) DEFAULT NULL AFTER unit_type,
    ADD COLUMN province         varchar(40) DEFAULT NULL AFTER address;
