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
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The rules inside a tenant's own data, rather than around its administration.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 12.44
 */
@SpringBootTest
class TenantDataRoleTest {

    private static final String OWNER_EMAIL = "data.owner@example.test";
    private static final String MEMBER_EMAIL = "data.member@example.test";
    private static final String OUTSIDER_EMAIL = "data.outsider@example.test";
    private static final String PASSWORD = "a-password-1";
    private static final String SLUG = "dataprobeclinic";

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
            tenantProvisioningService.provision(OrganizationProfile.ofName("Data Probe Clinic"), owner);
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

    private String personJson(String firstName) {
        return objectMapper.writeValueAsString(Map.of("firstName", firstName, "lastName", "Probe"));
    }

    private Long createPersonAsMember(String firstName) throws Exception {
        String body = mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personJson(firstName)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    /**
     * The daily work of the tenant. Withholding it would leave MEMBER useless.
     */
    @Test
    void aMemberMayReadAndWritePeople() throws Exception {
        Long id = createPersonAsMember("Written");

        mockMvc.perform(get("/person/" + id)
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Written"));

        mockMvc.perform(put("/person/" + id)
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personJson("Edited")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Edited"));
    }

    /**
     * A removed record is not something a shift should be able to undo.
     */
    @Test
    void onlyAnOwnerMayDeleteAPerson() throws Exception {
        Long id = createPersonAsMember("Doomed");

        mockMvc.perform(delete("/person/" + id)
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/person/" + id)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isNoContent());
    }

    /**
     * These are closer to the shape of the business than to its daily work.
     */
    @Test
    void aMemberMayReadButNotChangeOrganizations() throws Exception {
        mockMvc.perform(get("/organization")
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isOk());

        String organization = objectMapper.writeValueAsString(Map.of("name", "Branch", "email", "b@probe.test"));

        mockMvc.perform(post("/organization")
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(organization))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/organization")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(organization))
                .andExpect(status().isCreated());
    }

    /**
     * The rules are per tenant, not global: being an owner somewhere else buys
     * nothing here.
     */
    @Test
    void somebodyOutsideTheTenantReachesNothing() throws Exception {
        mockMvc.perform(get("/person")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personJson("Intruder")))
                .andExpect(status().isForbidden());
    }

    /**
     * Without a resolved tenant there is no role to hold, so writing has to be
     * refused rather than landing in the central database.
     */
    @Test
    void writingWithNoTenantResolvedIsRefused() throws Exception {
        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personJson("Nowhere")))
                .andExpect(status().isForbidden());
    }

    /**
     * The detail screen has to tell "no such person" apart from "the API is
     * unreachable", and a 200 with an empty body says neither.
     */
    @Test
    void readingAMissingRecordIsNotFoundRatherThanAnEmptyBody() throws Exception {
        mockMvc.perform(get("/person/999999")
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/organization/999999")
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isNotFound());
    }

    @Test
    void aMissingRecordIsNotFoundRatherThanAServerError() throws Exception {
        mockMvc.perform(put("/person/999999")
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personJson("Ghost")))
                .andExpect(status().isNotFound());
    }
}
