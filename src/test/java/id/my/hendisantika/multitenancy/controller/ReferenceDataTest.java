package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.config.TenantSubdomainInterceptor;
import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.AccountStatus;
import id.my.hendisantika.multitenancy.entity.central.TenantRole;
import id.my.hendisantika.multitenancy.repository.central.AccountRepository;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
import id.my.hendisantika.multitenancy.repository.central.UserTenantRepository;
import id.my.hendisantika.multitenancy.service.MembershipService;
import id.my.hendisantika.multitenancy.service.OrganizationProfile;
import id.my.hendisantika.multitenancy.service.TenantProvisioningService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The lists every tenant is given when its database is created.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 18.52
 */
@SpringBootTest
class ReferenceDataTest {

    private static final String OWNER_EMAIL = "reference.owner@example.test";
    private static final String MEMBER_EMAIL = "reference.member@example.test";
    private static final String OUTSIDER_EMAIL = "reference.outsider@example.test";
    private static final String PASSWORD = "a-password-1";
    private static final String SLUG = "referenceprobeclinic";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantProvisioningService tenantProvisioningService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private TenantRegistrationRepository tenantRegistrationRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserTenantRepository userTenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private String ownerToken;
    private String memberToken;
    private String outsiderToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        Account owner = account(OWNER_EMAIL);
        account(OUTSIDER_EMAIL);
        if (tenantRegistrationRepository.findBySlug(SLUG).isEmpty()) {
            tenantProvisioningService.provision(OrganizationProfile.ofName("Reference Probe Clinic"), owner);
            membershipService.addMember(SLUG, MEMBER_EMAIL, null, PASSWORD, TenantRole.MEMBER);
        }
        ownerToken = tokenFor(OWNER_EMAIL);
        memberToken = tokenFor(MEMBER_EMAIL);
        outsiderToken = tokenFor(OUTSIDER_EMAIL);
    }

    private Account account(String email) {
        return accountRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Account created = new Account();
            created.setEmail(email);
            created.setPassword(passwordEncoder.encode(PASSWORD));
            created.setStatus(AccountStatus.ACTIVE);
            created.setCreatedAt(Instant.now());
            created.setEmailVerifiedAt(Instant.now());
            return accountRepository.save(created);
        });
    }

    private String tokenFor(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asString();
    }

    @AfterEach
    void cleanUp() {
        if (tenantRegistrationRepository.findBySlug(SLUG).isPresent()) {
            tenantProvisioningService.deprovision(SLUG);
        }
        List.of(OWNER_EMAIL, MEMBER_EMAIL, OUTSIDER_EMAIL).forEach(email ->
                accountRepository.findByEmailIgnoreCase(email).ifPresent(account -> {
                    userTenantRepository.deleteAll(userTenantRepository.findAllByAccountId(account.getId()));
                    accountRepository.delete(account);
                }));
    }

    /**
     * The point of the whole change: a tenant provisioned a moment ago already
     * has the lists, without anybody importing anything.
     */
    @Test
    void aFreshlyProvisionedTenantAlreadyHasTheLists() throws Exception {
        mockMvc.perform(get("/reference-data")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.GENDER.length()").value(2))
                .andExpect(jsonPath("$.BLOOD_TYPE.length()").value(8))
                .andExpect(jsonPath("$.APPOINTMENT_STATUS.length()").value(7))
                .andExpect(jsonPath("$.PAYER_TYPE.length()").value(4))
                .andExpect(jsonPath("$.IDENTITY_DOCUMENT.length()").value(6))
                .andExpect(jsonPath("$.MARITAL_STATUS.length()").value(4))
                .andExpect(jsonPath("$.RELATIONSHIP.length()").value(6))
                .andExpect(jsonPath("$.VISIT_TYPE.length()").value(5));
    }

    /**
     * A dropdown in the order a clinic reads it, not the order MySQL happens to
     * hand back.
     */
    @Test
    void aCategoryComesBackInItsOwnOrder() throws Exception {
        mockMvc.perform(get("/reference-data/APPOINTMENT_STATUS")
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SCHEDULED"))
                .andExpect(jsonPath("$[1].code").value("CONFIRMED"))
                .andExpect(jsonPath("$[2].code").value("CHECKED_IN"))
                .andExpect(jsonPath("$[6].code").value("NO_SHOW"))
                .andExpect(jsonPath("$[6].label").value("Did not attend"))
                .andExpect(jsonPath("$[0].systemDefined").value(true));
    }

    /**
     * Somebody typing a category into a URL should not have to shout it.
     */
    @Test
    void aCategoryIsFoundWhateverCaseItIsAskedFor() throws Exception {
        mockMvc.perform(get("/reference-data/blood_type")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));
    }

    /**
     * A list this tenant does not keep is an empty dropdown, not a failure.
     */
    @Test
    void anUnknownCategoryIsEmptyRatherThanAnError() throws Exception {
        mockMvc.perform(get("/reference-data/NOT_A_CATEGORY")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * These are what the forms are filled in from, so a MEMBER who cannot read
     * them cannot do the work.
     */
    @Test
    void aMemberMayReadTheLists() throws Exception {
        mockMvc.perform(get("/reference-data")
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.GENDER.length()").value(2));
    }

    /**
     * Seeded into every tenant is not the same as public: the rules that guard
     * the rest of the tenant's data guard these too.
     */
    @Test
    void somebodyOutsideTheTenantReachesNothing() throws Exception {
        mockMvc.perform(get("/reference-data")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/reference-data")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }
}
