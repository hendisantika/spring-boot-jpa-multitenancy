package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.config.TenantContext;
import id.my.hendisantika.multitenancy.config.TenantDataSourceRegistry;
import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.entity.central.TenantStatus;
import id.my.hendisantika.multitenancy.entity.tenant.Organization;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
import id.my.hendisantika.multitenancy.repository.tenant.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the real provisioning path: a database is created, migrated and
 * routed to, without restarting the application.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 */
@SpringBootTest
class TenantProvisioningServiceTest {

    private static final String DISPLAY_NAME = "Provisioning Probe";
    private static final String SLUG = "provisioningprobe";

    @Autowired
    private TenantProvisioningService tenantProvisioningService;

    @Autowired
    private TenantRegistrationRepository tenantRegistrationRepository;

    @Autowired
    private TenantDataSourceRegistry tenantDataSourceRegistry;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private DataSource centralDataSource;

    @AfterEach
    void cleanUp() {
        TenantContext.clearTenant();
        tenantRegistrationRepository.findBySlug(SLUG)
                .ifPresent(tenant -> tenantProvisioningService.deprovision(SLUG));
    }

    @Test
    void provisionsDatabaseAndSubdomainForOrganization() {
        TenantRegistration tenant = tenantProvisioningService.provision(DISPLAY_NAME);

        assertThat(tenant.getSlug()).isEqualTo(SLUG);
        assertThat(tenant.getDatabaseName()).isEqualTo(SLUG);
        assertThat(tenant.getSubdomain()).isEqualTo(SLUG + ".mhdc.co.id");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);

        // The database really exists and carries the tenant schema.
        List<String> tables = new JdbcTemplate(centralDataSource).queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = ?",
                String.class, SLUG);
        assertThat(tables).contains("organizations", "persons", "users", "flyway_schema_history");
        // user_tenants is central only, so it must not be duplicated per tenant.
        assertThat(tables).doesNotContain("user_tenants");
    }

    @Test
    void routesRepositoriesToTheNewDatabaseWithoutRestart() {
        tenantProvisioningService.provision(DISPLAY_NAME);

        TenantContext.setTenant(SLUG);
        Organization organization = new Organization();
        organization.setName("Probe Clinic");
        organization.setEmail("probe@example.test");
        organizationRepository.saveAndFlush(organization);

        assertThat(organizationRepository.findAll()).extracting(Organization::getName)
                .containsExactly("Probe Clinic");

        // The row landed in the tenant database, not the central one.
        TenantContext.clearTenant();
        Integer centralRows = new JdbcTemplate(centralDataSource).queryForObject(
                "SELECT COUNT(*) FROM organizations WHERE name = ?", Integer.class, "Probe Clinic");
        assertThat(centralRows).isZero();
    }

    @Test
    void rejectsReservedAndDuplicateNames() {
        assertThatThrownBy(() -> tenantProvisioningService.provision("mysql"))
                .isInstanceOf(TenantProvisioningException.class)
                .hasMessageContaining("reserved");

        tenantProvisioningService.provision(DISPLAY_NAME);
        assertThatThrownBy(() -> tenantProvisioningService.provision(DISPLAY_NAME))
                .isInstanceOf(TenantProvisioningException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void namesCannotSmuggleSqlIntoTheCreateDatabaseStatement() {
        // Slugifying strips every character that could terminate an identifier or a
        // statement, so the payload can never reach DDL.
        assertThat(TenantSlugs.slugify("Sehat`; DROP DATABASE db_default; --"))
                .isEqualTo("sehatdropdatabasedbdefault");
        assertThat(TenantSlugs.isValid("sehat`; DROP DATABASE db_default")).isFalse();

        // And the central database is still there.
        Integer centralExists = new JdbcTemplate(centralDataSource).queryForObject(
                "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?",
                Integer.class, "db_default");
        assertThat(centralExists).isEqualTo(1);
    }

    @Test
    void refusesToAdoptAnExistingUnrelatedDatabase() {
        JdbcTemplate jdbc = new JdbcTemplate(centralDataSource);
        jdbc.execute("CREATE DATABASE IF NOT EXISTS `squatterdatabase`");
        try {
            // A tenant name that maps onto a database somebody else owns must not
            // silently take it over and migrate it.
            assertThatThrownBy(() -> tenantProvisioningService.provision("Squatter Database"))
                    .isInstanceOf(TenantProvisioningException.class)
                    .hasMessageContaining("already exists");
            assertThat(tenantRegistrationRepository.findBySlug("squatterdatabase")).isEmpty();
        } finally {
            jdbc.execute("DROP DATABASE IF EXISTS `squatterdatabase`");
        }
    }

    @Test
    void rejectsNamesThatWouldNotBeSafeAsIdentifiers() {
        // Collapses to an empty slug.
        assertThatThrownBy(() -> tenantProvisioningService.provision("!!!"))
                .isInstanceOf(TenantProvisioningException.class);
        // Shorter than a DNS label we are willing to hand out.
        assertThatThrownBy(() -> tenantProvisioningService.provision("ab"))
                .isInstanceOf(TenantProvisioningException.class);
        // Must start with a letter to be a legal DNS label.
        assertThatThrownBy(() -> tenantProvisioningService.provision("1clinic"))
                .isInstanceOf(TenantProvisioningException.class);
    }

    @Test
    @Transactional("centralTransactionManager")
    void unknownTenantIsNotRoutable() {
        assertThatThrownBy(() -> tenantDataSourceRegistry.resolve("nosuchtenant"))
                .isInstanceOf(id.my.hendisantika.multitenancy.config.UnknownTenantException.class);
    }
}
