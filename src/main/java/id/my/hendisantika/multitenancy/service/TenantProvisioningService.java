package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.config.DatabaseProperties;
import id.my.hendisantika.multitenancy.config.HibernateSettings;
import id.my.hendisantika.multitenancy.config.TenantDataSourceRegistry;
import id.my.hendisantika.multitenancy.config.TenantProperties;
import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.entity.central.TenantStatus;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final TenantDataSourceRegistry tenantDataSourceRegistry;
    private final TenantProperties tenantProperties;
    private final DatabaseProperties databaseProperties;

    /**
     * @param displayName organization name as the owner typed it, e.g. "Sehat"
     * @return the registered tenant, with its database created and migrated
     */
    @Transactional("centralTransactionManager")
    public TenantRegistration provision(String displayName) {
        String slug = TenantSlugs.slugify(displayName);
        validate(slug);

        TenantRegistration tenant = new TenantRegistration();
        tenant.setSlug(slug);
        tenant.setDatabaseName(slug);
        tenant.setSubdomain(slug + "." + tenantProperties.getBaseDomain());
        tenant.setDisplayName(displayName);
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
        try (Connection connection = tenantDataSourceRegistry.getCentralDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP DATABASE IF EXISTS `%s`".formatted(tenant.getDatabaseName()));
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new TenantProvisioningException("Could not drop database " + tenant.getDatabaseName(), e);
        }
        tenantRegistrationRepository.delete(tenant);
        log.info("Deprovisioned tenant {}", normalized);
    }
}
