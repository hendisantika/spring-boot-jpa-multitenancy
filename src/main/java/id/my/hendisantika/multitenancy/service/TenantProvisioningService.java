package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.config.DatabaseProperties;
import id.my.hendisantika.multitenancy.config.HibernateSettings;
import id.my.hendisantika.multitenancy.config.TenantDataSourceRegistry;
import id.my.hendisantika.multitenancy.config.TenantProperties;
import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.entity.central.TenantStatus;
import id.my.hendisantika.multitenancy.entity.central.TenantRole;
import id.my.hendisantika.multitenancy.entity.central.UserTenant;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
import id.my.hendisantika.multitenancy.repository.central.UserTenantRepository;
import id.my.hendisantika.multitenancy.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Creates a database and a subdomain for a new organization, then publishes it so
 * that requests can be served without a restart.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantDataSourceRegistry tenantDataSourceRegistry;
    private final TenantProperties tenantProperties;
    private final DatabaseProperties databaseProperties;
    private final StorageService storageService;

    /**
     * @param displayName organization name as the owner typed it, e.g. "Sehat"
     * @return the registered tenant, with its database created and migrated
     */
    @Transactional("centralTransactionManager")
    public TenantRegistration provision(String displayName) {
        return provision(OrganizationProfile.ofName(displayName), null);
    }

    /**
     * @param profile the registration form; only the business name is required,
     *                the rest is profile detail
     * @param owner   the account registering the organization, which becomes its
     *                OWNER member
     */
    @Transactional("centralTransactionManager")
    public TenantRegistration provision(OrganizationProfile profile, Account owner) {
        String slug = TenantSlugs.slugify(profile.businessName());
        validate(slug);

        TenantRegistration tenant = new TenantRegistration();
        tenant.setSlug(slug);
        tenant.setDatabaseName(slug);
        tenant.setSubdomain(slug + "." + tenantProperties.getBaseDomain());
        tenant.setDisplayName(profile.businessName());
        tenant.setBusinessEmail(profile.businessEmail());
        tenant.setPhotoKey(profile.photoKey());
        tenant.setContactFirstName(profile.contactFirstName());
        tenant.setContactLastName(profile.contactLastName());
        tenant.setJobTitle(profile.jobTitle());
        tenant.setPhoneNumber(profile.phoneNumber());
        tenant.setOrgStructure(profile.orgStructure());
        tenant.setPracticeSpeciality(profile.practiceSpeciality());
        tenant.setOwner(owner);
        tenant.setStatus(TenantStatus.PROVISIONING);
        tenant.setCreatedAt(Instant.now());
        tenant = tenantRegistrationRepository.saveAndFlush(tenant);

        createDatabase(tenant.getDatabaseName());
        try {
            migrate(tenant);
        } catch (RuntimeException e) {
            // The registry row rolls back with the transaction, but DDL does not, so
            // clean up the database we just created rather than leaving an orphan.
            dropDatabaseQuietly(tenant.getDatabaseName());
            throw new TenantProvisioningException(
                    "Could not migrate database " + tenant.getDatabaseName(), e);
        }

        tenant.setStatus(TenantStatus.ACTIVE);
        tenant = tenantRegistrationRepository.saveAndFlush(tenant);
        tenantDataSourceRegistry.open(tenant);

        if (owner != null) {
            // The registering account becomes the owner member, which is what the
            // access token will carry on the next login.
            UserTenant membership = new UserTenant();
            membership.setAccount(owner);
            membership.setUserName(owner.getEmail());
            membership.setTenantSlug(tenant.getSlug());
            membership.setRole(TenantRole.OWNER);
            userTenantRepository.save(membership);
        }

        log.info("Provisioned tenant {} on database {} at {}",
                tenant.getSlug(), tenant.getDatabaseName(), tenant.getSubdomain());
        return tenant;
    }

    private void validate(String slug) {
        if (!TenantSlugs.isValid(slug)) {
            throw new TenantProvisioningException(
                    "'" + slug + "' is not a usable tenant name: it must start with a letter, "
                            + "contain only letters and digits and be 3 to 30 characters long");
        }
        if (tenantProperties.getReservedSlugs().contains(slug)) {
            throw new TenantProvisioningException("'" + slug + "' is reserved");
        }
        if (tenantRegistrationRepository.existsBySlugOrDatabaseName(slug, slug)) {
            throw new TenantProvisioningException("'" + slug + "' is already taken");
        }
    }

    /**
     * The slug is validated against {@link TenantSlugs#VALID_SLUG} before it gets
     * here, which is what makes interpolating it into DDL safe: MySQL cannot bind
     * an identifier as a parameter.
     */
    private void createDatabase(String databaseName) {
        if (!TenantSlugs.isValid(databaseName)) {
            throw new TenantProvisioningException("Refusing to create database '" + databaseName + "'");
        }
        try (Connection connection = tenantDataSourceRegistry.getCentralDataSource().getConnection()) {
            // Tenant databases are named after the slug alone, so they share the
            // server's namespace with unrelated schemas. Adopting one of those and
            // migrating it would corrupt somebody else's data, so refuse instead of
            // using CREATE DATABASE IF NOT EXISTS.
            if (databaseExists(connection, databaseName)) {
                throw new TenantProvisioningException(
                        "Database '" + databaseName + "' already exists on this server and is not "
                                + "registered to a tenant; choose a different organization name");
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "CREATE DATABASE `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                                .formatted(databaseName));
            }
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new TenantProvisioningException("Could not create database " + databaseName, e);
        }
    }

    private boolean databaseExists(Connection connection, String databaseName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.schemata WHERE schema_name = ?")) {
            statement.setString(1, databaseName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void dropDatabaseQuietly(String databaseName) {
        if (!TenantSlugs.isValid(databaseName)) {
            return;
        }
        try (Connection connection = tenantDataSourceRegistry.getCentralDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP DATABASE IF EXISTS `%s`".formatted(databaseName));
        } catch (SQLException e) {
            log.warn("Could not roll back creation of database {}", databaseName, e);
        }
    }

    private void migrate(TenantRegistration tenant) {
        DataSource dataSource = tenantDataSourceRegistry.open(tenant);
        Flyway.configure()
                .locations(HibernateSettings.TENANT_MIGRATION_LOCATION)
                .baselineOnMigrate(Boolean.TRUE)
                .dataSource(dataSource)
                .schemas(tenant.getDatabaseName())
                .load()
                .migrate();
    }

    /**
     * Drops a tenant database. Intended for tests and for undoing a failed
     * provisioning run, not for routine use.
     * <p>
     * The photos go too. Every delete endpoint removes the object beside the row
     * that pointed at it, and this used to be the one path that did not: it
     * dropped the database holding the keys, so the objects stayed in the bucket
     * with nothing left to say whose they were or that they could go.
     */
    @Transactional("centralTransactionManager")
    public void deprovision(String slug) {
        String normalized = slug == null ? "" : slug.toLowerCase(Locale.ROOT);
        TenantRegistration tenant = tenantRegistrationRepository.findBySlug(normalized)
                .orElseThrow(() -> new TenantProvisioningException("'" + normalized + "' is not registered"));
        if (!TenantSlugs.isValid(tenant.getDatabaseName())) {
            throw new TenantProvisioningException("Refusing to drop database '" + tenant.getDatabaseName() + "'");
        }
        if (tenant.getDatabaseName().equals(databaseProperties.getCentralDatabase())) {
            throw new TenantProvisioningException("Refusing to drop the central database");
        }

        // Read before dropping: afterwards there is nothing left to read them
        // from. The name is safe to interpolate only because it was checked
        // against TenantSlugs just above.
        List<String> photoKeys = photoKeysOf(tenant);

        try (Connection connection = tenantDataSourceRegistry.getCentralDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP DATABASE IF EXISTS `%s`".formatted(tenant.getDatabaseName()));
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new TenantProvisioningException("Could not drop database " + tenant.getDatabaseName(), e);
        }
        // Memberships reference the slug rather than the row, so they have to go too.
        userTenantRepository.deleteAll(userTenantRepository.findAllByTenantSlug(normalized));
        tenantRegistrationRepository.delete(tenant);

        // After the drop, in the order the record deletes use: losing a photo
        // while the tenant still exists is worse than a bucket that lags by one
        // failed call, and by here there is nothing left to roll back to.
        for (String key : photoKeys) {
            try {
                storageService.delete(key);
            } catch (RuntimeException e) {
                log.warn("Could not delete {} while deprovisioning {}", key, normalized, e);
            }
        }
        log.info("Deprovisioned tenant {}, removing {} photo(s)", normalized, photoKeys.size());
    }

    /**
     * Every object this tenant owns: its own logo, held centrally, plus the
     * photo of every person and unit in its database.
     *
     * @return best effort — a database that is already gone, or predates the
     * photo columns, simply has nothing to contribute, and that is not a reason
     * to refuse to deprovision
     */
    private List<String> photoKeysOf(TenantRegistration tenant) {
        List<String> keys = new ArrayList<>();
        if (tenant.getPhotoKey() != null && !tenant.getPhotoKey().isBlank()) {
            keys.add(tenant.getPhotoKey());
        }
        for (String table : new String[]{"persons", "organizations"}) {
            String sql = "SELECT photo_key FROM `%s`.`%s` WHERE photo_key IS NOT NULL"
                    .formatted(tenant.getDatabaseName(), table);
            try (Connection connection = tenantDataSourceRegistry.getCentralDataSource().getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(sql)) {
                while (rows.next()) {
                    String key = rows.getString(1);
                    if (key != null && !key.isBlank()) {
                        keys.add(key);
                    }
                }
            } catch (SQLException e) {
                log.warn("Could not read photo keys from {}.{}, leaving those objects alone",
                        tenant.getDatabaseName(), table, e);
            }
        }
        return keys;
    }
}
