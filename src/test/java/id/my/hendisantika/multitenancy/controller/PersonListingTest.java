package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.config.TenantSubdomainInterceptor;
import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.AccountStatus;
import id.my.hendisantika.multitenancy.repository.central.AccountRepository;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
import id.my.hendisantika.multitenancy.repository.central.UserTenantRepository;
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
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paging and searching the people in a tenant's own database.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 14.18
 */
@SpringBootTest
class PersonListingTest {

    private static final String OWNER_EMAIL = "listing.owner@example.test";
    private static final String PASSWORD = "a-password-1";
    private static final String SLUG = "listingprobeclinic";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantProvisioningService tenantProvisioningService;

    @Autowired
    private TenantRegistrationRepository tenantRegistrationRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserTenantRepository userTenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        Account owner = accountRepository.findByEmailIgnoreCase(OWNER_EMAIL).orElseGet(() -> {
            Account created = new Account();
            created.setEmail(OWNER_EMAIL);
            created.setPassword(passwordEncoder.encode(PASSWORD));
            created.setStatus(AccountStatus.ACTIVE);
            created.setCreatedAt(Instant.now());
            created.setEmailVerifiedAt(Instant.now());
            return accountRepository.save(created);
        });
        if (tenantRegistrationRepository.findBySlug(SLUG).isEmpty()) {
            tenantProvisioningService.provision(OrganizationProfile.ofName("Listing Probe Clinic"), owner);
        }

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.LoginRequest(OWNER_EMAIL, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(body).get("accessToken").asString();
    }

    @AfterEach
    void cleanUp() {
        if (tenantRegistrationRepository.findBySlug(SLUG).isPresent()) {
            tenantProvisioningService.deprovision(SLUG);
        }
        accountRepository.findByEmailIgnoreCase(OWNER_EMAIL).ifPresent(account -> {
            userTenantRepository.deleteAll(userTenantRepository.findAllByAccountId(account.getId()));
            accountRepository.delete(account);
        });
    }

    private MockHttpServletRequestBuilder asOwner(MockHttpServletRequestBuilder request) {
        return request
                .header("Authorization", "Bearer " + token)
                .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG);
    }

    private void createPerson(String firstName, String lastName, String email, String mobile) throws Exception {
        Map<String, String> person = new LinkedHashMap<>();
        person.put("firstName", firstName);
        person.put("lastName", lastName);
        person.put("email", email);
        person.put("mobile", mobile);
        mockMvc.perform(asOwner(post("/person"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(person)))
                .andExpect(status().isCreated());
    }

    private void createPeople(int count) throws Exception {
        for (int i = 1; i <= count; i++) {
            createPerson("Person" + i, "Probe", "person" + i + "@probe.test", "08" + i);
        }
    }

    /**
     * The point of a page: the slice is short, but the count is of everybody.
     */
    @Test
    void aPageCarriesItsSliceAndTheWholeCount() throws Exception {
        createPeople(7);

        mockMvc.perform(asOwner(get("/person")).param("page", "0").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content[0].firstName").value("Person1"));

        mockMvc.perform(asOwner(get("/person")).param("page", "2").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Person7"));
    }

    /**
     * Paging only means anything if the order holds still between requests.
     */
    @Test
    void thePagesDoNotOverlap() throws Exception {
        createPeople(6);

        String first = mockMvc.perform(asOwner(get("/person")).param("page", "0").param("size", "3"))
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(asOwner(get("/person")).param("page", "1").param("size", "3"))
                .andReturn().getResponse().getContentAsString();

        var firstIds = objectMapper.readTree(first).get("content").findValues("id");
        var secondIds = objectMapper.readTree(second).get("content").findValues("id");
        org.assertj.core.api.Assertions.assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
    }

    /**
     * Past the end is an empty page, not a failure: a stale link should not
     * throw an error at somebody.
     */
    @Test
    void askingBeyondTheLastPageIsEmptyRatherThanAnError() throws Exception {
        createPeople(2);

        mockMvc.perform(asOwner(get("/person")).param("page", "9").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    /**
     * Nobody should be able to ask for the whole database in one request.
     */
    @Test
    void theRequestedSizeIsClamped() throws Exception {
        createPeople(3);

        mockMvc.perform(asOwner(get("/person")).param("size", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(200));

        mockMvc.perform(asOwner(get("/person")).param("size", "0").param("page", "-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    /**
     * Somebody typing a name types both halves of it, not one column.
     */
    @Test
    void aSearchMatchesTheFullNameAsWellAsEitherHalf() throws Exception {
        createPerson("Budi", "Santoso", "budi@probe.test", "0811");
        createPerson("Siti", "Rahayu", "siti@probe.test", "0822");

        mockMvc.perform(asOwner(get("/person")).param("q", "budi santoso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Budi"));

        mockMvc.perform(asOwner(get("/person")).param("q", "rahayu"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Siti"));
    }

    /**
     * People search in whatever case they happen to be typing, and by whichever
     * detail they remember.
     */
    @Test
    void aSearchIgnoresCaseAndReachesEmailAndMobile() throws Exception {
        createPerson("Budi", "Santoso", "budi@probe.test", "081234");

        mockMvc.perform(asOwner(get("/person")).param("q", "BUDI"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(asOwner(get("/person")).param("q", "@probe.test"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(asOwner(get("/person")).param("q", "1234"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    /**
     * A blank box is not a search for nothing, it is everybody.
     */
    @Test
    void aBlankSearchIsEverybody() throws Exception {
        createPeople(4);

        mockMvc.perform(asOwner(get("/person")).param("q", "   "))
                .andExpect(jsonPath("$.totalElements").value(4));
        mockMvc.perform(asOwner(get("/person")))
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    /**
     * A percent sign is a character in somebody's name, not an instruction to
     * the database.
     */
    @Test
    void wildcardsTypedByAUserAreLiteral() throws Exception {
        createPerson("Budi", "Santoso", "budi@probe.test", "0811");
        createPerson("100%", "Real", "real@probe.test", "0822");

        mockMvc.perform(asOwner(get("/person")).param("q", "%"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("100%"));

        mockMvc.perform(asOwner(get("/person")).param("q", "_"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /**
     * A search reaching the wrong database would be the whole point of this
     * project failing quietly, so paging carries the tenant like anything else.
     */
    @Test
    void aSearchWithNoTenantResolvedIsRefused() throws Exception {
        mockMvc.perform(get("/person")
                        .header("Authorization", "Bearer " + token)
                        .param("q", "budi"))
                .andExpect(status().isForbidden());
    }
}
