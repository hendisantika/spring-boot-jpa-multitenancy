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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paging and searching the business units in a tenant's own database.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 15.24
 */
@SpringBootTest
class UnitListingTest {

    private static final String OWNER_EMAIL = "units.owner@example.test";
    private static final String MEMBER_EMAIL = "units.member@example.test";
    private static final String PASSWORD = "a-password-1";
    private static final String SLUG = "unitprobeclinic";

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

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        Account owner = account(OWNER_EMAIL);
        if (tenantRegistrationRepository.findBySlug(SLUG).isEmpty()) {
            tenantProvisioningService.provision(OrganizationProfile.ofName("Unit Probe Clinic"), owner);
            membershipService.addMember(SLUG, MEMBER_EMAIL, null, PASSWORD, TenantRole.MEMBER);
        }
        ownerToken = tokenFor(OWNER_EMAIL);
        memberToken = tokenFor(MEMBER_EMAIL);
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
        List.of(OWNER_EMAIL, MEMBER_EMAIL).forEach(email ->
                accountRepository.findByEmailIgnoreCase(email).ifPresent(account -> {
                    userTenantRepository.deleteAll(userTenantRepository.findAllByAccountId(account.getId()));
                    accountRepository.delete(account);
                }));
    }

    private MockHttpServletRequestBuilder withTenant(MockHttpServletRequestBuilder request, String token) {
        return request
                .header("Authorization", "Bearer " + token)
                .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG);
    }

    private void createUnit(String name, String address, String email) throws Exception {
        Map<String, String> unit = new LinkedHashMap<>();
        unit.put("name", name);
        unit.put("address", address);
        unit.put("email", email);
        mockMvc.perform(withTenant(post("/organization"), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unit)))
                .andExpect(status().isCreated());
    }

    private void createUnits(int count) throws Exception {
        for (int i = 1; i <= count; i++) {
            createUnit("Cabang " + i, "Jalan " + i, "cabang" + i + "@probe.test");
        }
    }

    @Test
    void aPageCarriesItsSliceAndTheWholeCount() throws Exception {
        createUnits(7);

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("page", "0").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].name").value("Cabang 1"));

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("page", "2").param("size", "3"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Cabang 7"));
    }

    @Test
    void thePagesDoNotOverlap() throws Exception {
        createUnits(6);

        String firstPage = mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("page", "0").param("size", "3"))
                .andReturn().getResponse().getContentAsString();
        String secondPage = mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("page", "1").param("size", "3"))
                .andReturn().getResponse().getContentAsString();

        var firstIds = objectMapper.readTree(firstPage).get("content").findValues("id");
        var secondIds = objectMapper.readTree(secondPage).get("content").findValues("id");
        org.assertj.core.api.Assertions.assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
    }

    @Test
    void askingBeyondTheLastPageIsEmptyRatherThanAnError() throws Exception {
        createUnits(2);

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("page", "9").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    /**
     * The same clamp as the people list, from the same place.
     */
    @Test
    void theRequestedSizeIsClamped() throws Exception {
        createUnits(3);

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("size", "100000"))
                .andExpect(jsonPath("$.size").value(200));
        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("size", "0").param("page", "-3"))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    /**
     * Whichever of the three details somebody remembers should find the unit.
     */
    @Test
    void aSearchReachesTheNameAddressAndEmail() throws Exception {
        createUnit("Cabang Pusat", "Jakarta Selatan", "pusat@probe.test");
        createUnit("Cabang Timur", "Surabaya", "timur@probe.test");

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "pusat"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Cabang Pusat"));
        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "SURABAYA"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Cabang Timur"));
        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "timur@probe"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "cabang"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void aBlankSearchIsEverything() throws Exception {
        createUnits(4);

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "   "))
                .andExpect(jsonPath("$.totalElements").value(4));
        mockMvc.perform(withTenant(get("/organization"), ownerToken))
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void wildcardsTypedByAUserAreLiteral() throws Exception {
        createUnit("Cabang Pusat", "Jakarta", "pusat@probe.test");
        createUnit("Diskon 100%", "Bandung", "diskon@probe.test");

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "%"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Diskon 100%"));
        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "_"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /**
     * Reading stays a member's right, so searching has to be as well: a list
     * they cannot narrow is a list they cannot use.
     */
    @Test
    void aMemberMaySearchEvenThoughTheyMayNotWrite() throws Exception {
        createUnit("Cabang Pusat", "Jakarta", "pusat@probe.test");
        createUnit("Cabang Timur", "Surabaya", "timur@probe.test");

        mockMvc.perform(withTenant(get("/organization"), memberToken).param("q", "pusat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void aSearchWithNoTenantResolvedIsRefused() throws Exception {
        mockMvc.perform(get("/organization")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("q", "cabang"))
                .andExpect(status().isForbidden());
    }
}
