package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.config.TenantContext;
import id.my.hendisantika.multitenancy.config.TenantSubdomainInterceptor;
import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.AccountStatus;
import id.my.hendisantika.multitenancy.entity.central.TenantRole;
import id.my.hendisantika.multitenancy.entity.tenant.ReferenceData;
import id.my.hendisantika.multitenancy.repository.central.AccountRepository;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
import id.my.hendisantika.multitenancy.repository.central.UserTenantRepository;
import id.my.hendisantika.multitenancy.repository.tenant.ReferenceDataRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private ReferenceDataRepository referenceDataRepository;

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

    private String personJson(String field, String code) {
        return "{\"firstName\":\"Probe\",\"lastName\":\"Person\",\"" + field + "\":\"" + code + "\"}";
    }

    /**
     * What a clinic would do through whatever administration screen eventually
     * exists: stop offering a value without deleting it.
     */
    private void retirePassport() {
        TenantContext.setTenant(SLUG);
        try {
            ReferenceData passport = referenceDataRepository
                    .findByCategoryOrderBySortOrderAsc("IDENTITY_DOCUMENT").stream()
                    .filter(value -> value.getCode().equals("PASSPORT"))
                    .findFirst()
                    .orElseThrow();
            passport.setActive(false);
            referenceDataRepository.save(passport);
        } finally {
            TenantContext.clearTenant();
        }
    }

    /**
     * The form offers a dropdown, but the request can be sent without one, so
     * the code has to be checked where it is stored rather than where it is
     * chosen.
     */
    @Test
    void aCodeOutsideTheListIsRefused() throws Exception {
        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personJson("bloodType", "MADE_UP")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("blood type")));

        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personJson("gender", "ROBOT")))
                .andExpect(status().isBadRequest());
    }

    /**
     * A code that belongs to a different list is still not a value for this one.
     */
    @Test
    void aCodeFromAnotherCategoryIsRefused() throws Exception {
        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personJson("gender", "O_POSITIVE")))
                .andExpect(status().isBadRequest());
    }

    /**
     * These fields are optional, so nothing is a real answer. Blank is stored as
     * nothing rather than as an empty string pretending to be a code.
     */
    @Test
    void aBlankCodeIsAcceptedAndStoredAsNothing() throws Exception {
        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personJson("gender", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gender").doesNotExist());
    }

    /**
     * What the dropdown offers is what the record accepts, and it comes back the
     * way it went in.
     */
    @Test
    void aCodeFromTheListIsStoredAndReadBack() throws Exception {
        String body = mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Budi","lastName":"Santoso","gender":"MALE",
                                 "maritalStatus":"MARRIED","bloodType":"O_POSITIVE",
                                 "identityDocumentType":"KTP","identityNumber":"3201234567890001"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.bloodType").value("O_POSITIVE"))
                .andExpect(jsonPath("$.identityNumber").value("3201234567890001"))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/person/" + id)
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(jsonPath("$.maritalStatus").value("MARRIED"))
                .andExpect(jsonPath("$.identityDocumentType").value("KTP"));
    }

    /**
     * Somebody typing into an API client should not have to shout, and the
     * stored code is the canonical one either way.
     */
    @Test
    void aCodeIsAcceptedInAnyCaseAndStoredAsTheCanonicalOne() throws Exception {
        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personJson("bloodType", "ab_negative")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloodType").value("AB_NEGATIVE"));
    }

    /**
     * Editing goes through the same check as creating: a record cannot be
     * corrupted on the way past the first one.
     */
    @Test
    void anEditIsCheckedTheSameWayAsACreate() throws Exception {
        String body = mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personJson("gender", "FEMALE")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(put("/person/" + id)
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personJson("gender", "NOT_A_GENDER")))
                .andExpect(status().isBadRequest());
    }

    /**
     * A value can be retired, but a record written while it was current still
     * holds its code. The list keeps the row so a client can still put a label
     * on it; what it must not do is let anybody choose it again.
     */
    @Test
    void aRetiredValueIsStillReadableButNoLongerAcceptable() throws Exception {
        String body = mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personJson("identityDocumentType", "PASSPORT")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();

        retirePassport();

        // Still listed, still labelled, but flagged rather than silently gone.
        mockMvc.perform(get("/reference-data/IDENTITY_DOCUMENT")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(jsonPath("$[?(@.code == 'PASSPORT')].label").value("Passport"))
                .andExpect(jsonPath("$[?(@.code == 'PASSPORT')].active").value(false));

        // The record it was written into is untouched.
        mockMvc.perform(get("/person/" + id)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(jsonPath("$.identityDocumentType").value("PASSPORT"));

        // But nobody may choose it again.
        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personJson("identityDocumentType", "PASSPORT")))
                .andExpect(status().isBadRequest());
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
