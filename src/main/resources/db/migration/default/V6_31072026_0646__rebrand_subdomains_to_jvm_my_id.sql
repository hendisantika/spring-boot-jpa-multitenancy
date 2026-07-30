-- The base domain moved from mhdc.co.id to jvm.my.id. Subdomains are stored on
-- the tenant row, so rows registered under the old domain have to be rewritten.
--
-- This is a new migration rather than an edit of V3: that one has already run,
-- and changing an applied migration breaks its checksum.
UPDATE tenants
SET subdomain = CONCAT(slug, '.jvm.my.id')
WHERE subdomain LIKE '%.mhdc.co.id';
