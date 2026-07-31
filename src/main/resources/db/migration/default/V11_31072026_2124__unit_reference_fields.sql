-- The column half of the tenant migration of this name.
--
-- A request that resolves to no tenant falls back to the central database
-- (DynamicRoutingDataSource), so the Organization entity is mapped against this
-- organizations table as well as every tenant's. The reference rows themselves
-- are not seeded here: reference_data is a tenant table, and this database has
-- no such list to keep.
ALTER TABLE organizations
    ADD COLUMN unit_type        varchar(40) DEFAULT NULL AFTER name,
    ADD COLUMN operating_status varchar(40) DEFAULT NULL AFTER unit_type,
    ADD COLUMN province         varchar(40) DEFAULT NULL AFTER address;
