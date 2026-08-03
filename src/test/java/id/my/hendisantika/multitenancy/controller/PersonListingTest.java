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
import id.my.hendisantika.multitenancy.service.storage.StorageService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    /**
     * Mocked rather than reaching MinIO: these tests are about how a photo is
     * wired through the controller and the service, not about the bucket, and
     * S3StorageIntegrationTest already covers the real thing.
     * <p>
     * They passed locally against a bucket left over from earlier work and
     * failed in CI, which starts with none — a real dependency that had no
     * business being here.
     */
    @MockitoBean
    private StorageService storageService;

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

    private MockMultipartHttpServletRequestBuilder asOwnerMultipart(MockMultipartHttpServletRequestBuilder request) {
        request.header("Authorization", "Bearer " + token)
                .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG);
        return request;
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

    private void createMarried(String firstName, String gender, String maritalStatus) throws Exception {
        Map<String, String> person = new LinkedHashMap<>();
        person.put("firstName", firstName);
        person.put("lastName", "Probe");
        person.put("gender", gender);
        person.put("maritalStatus", maritalStatus);
        mockMvc.perform(asOwner(post("/person"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(person)))
                .andExpect(status().isCreated());
    }

    private void createCodedPerson(String firstName, String gender, String blood, String document)
            throws Exception {
        Map<String, String> person = new LinkedHashMap<>();
        person.put("firstName", firstName);
        person.put("lastName", "Probe");
        person.put("gender", gender);
        person.put("bloodType", blood);
        person.put("identityDocumentType", document);
        mockMvc.perform(asOwner(post("/person"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(person)))
                .andExpect(status().isCreated());
    }

    /**
     * The record stores O_POSITIVE and MALE. Nobody types that, so the search
     * has to reach the label a clinic actually reads — the same way the units
     * list does.
     */
    @Test
    void aSearchReachesTheCodedFieldsByTheirLabel() throws Exception {
        createCodedPerson("Satu", "MALE", "O_POSITIVE", "KTP");
        createCodedPerson("Dua", "FEMALE", "AB_NEGATIVE", "PASSPORT");
        createCodedPerson("Tiga", "FEMALE", "O_POSITIVE", "KITAS");

        mockMvc.perform(asOwner(get("/person")).param("q", "Female"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(asOwner(get("/person")).param("q", "AB-"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Dua"));

        mockMvc.perform(asOwner(get("/person")).param("q", "passport"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Dua"));

        mockMvc.perform(asOwner(get("/person")).param("q", "KITAS"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Tiga"));
    }

    /**
     * The same line as the units list: the label is searched, never the code.
     * Codes contain underscores, and matching them would make a typed "_" find
     * nearly everybody while the free-text half treats it as a literal.
     */
    @Test
    void theStoredCodeIsNotWhatIsSearched() throws Exception {
        createCodedPerson("Satu", "MALE", "O_POSITIVE", "KTP");

        mockMvc.perform(asOwner(get("/person")).param("q", "O_POSITIVE"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(asOwner(get("/person")).param("q", "_"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(asOwner(get("/person")).param("q", "%"))
                .andExpect(jsonPath("$.totalElements").value(0));

        // The label it stands for does find them.
        mockMvc.perform(asOwner(get("/person")).param("q", "O+"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    /**
     * A term matching no label must not quietly match everybody, which is what
     * an empty {@code in} clause would do if it were written carelessly.
     */
    @Test
    void aTermMatchingNoLabelStillFindsNobody() throws Exception {
        createCodedPerson("Satu", "MALE", "O_POSITIVE", "KTP");
        createCodedPerson("Dua", "FEMALE", "AB_NEGATIVE", "PASSPORT");

        mockMvc.perform(asOwner(get("/person")).param("q", "zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /**
     * Somebody with nothing coded is still found by name, and is not swept up by
     * a search for a label they do not carry.
     */
    @Test
    void aPersonWithNoCodesIsUnaffected() throws Exception {
        createPerson("Polos", "Tanpa", "polos@probe.test", "0899");
        createCodedPerson("Berkode", "FEMALE", "AB_NEGATIVE", "PASSPORT");

        mockMvc.perform(asOwner(get("/person")).param("q", "polos"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Polos"));

        mockMvc.perform(asOwner(get("/person")).param("q", "Female"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Berkode"));
    }

    /**
     * A filter narrows on one field, exactly, and takes the code rather than the
     * label — the opposite of the search next to it.
     */
    @Test
    void aFilterNarrowsOnOneFieldExactly() throws Exception {
        createCodedPerson("Satu", "MALE", "O_POSITIVE", "KTP");
        createCodedPerson("Dua", "FEMALE", "AB_NEGATIVE", "PASSPORT");
        createCodedPerson("Tiga", "FEMALE", "O_POSITIVE", "KITAS");

        mockMvc.perform(asOwner(get("/person")).param("gender", "FEMALE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(asOwner(get("/person")).param("bloodType", "o_positive"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(asOwner(get("/person")).param("identityDocumentType", "PASSPORT"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Dua"));
    }

    /**
     * Two filters mean both, and a filter narrows a search rather than widening
     * it. Getting that backwards would quietly return more than was asked for.
     */
    @Test
    void filtersCombineWithEachOtherAndWithTheSearch() throws Exception {
        createCodedPerson("Satu", "MALE", "O_POSITIVE", "KTP");
        createCodedPerson("Dua", "FEMALE", "AB_NEGATIVE", "PASSPORT");
        createCodedPerson("Tiga", "FEMALE", "O_POSITIVE", "KITAS");

        mockMvc.perform(asOwner(get("/person"))
                        .param("gender", "FEMALE")
                        .param("bloodType", "O_POSITIVE"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Tiga"));

        // The search alone finds both women; the filter cuts it to one.
        mockMvc.perform(asOwner(get("/person")).param("q", "Female"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(asOwner(get("/person"))
                        .param("q", "Female")
                        .param("bloodType", "AB_NEGATIVE"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Dua"));
    }

    /**
     * "O+ or O−" is a real question in a clinic, so blood type takes several at
     * once and they mean either.
     */
    @Test
    void severalBloodTypesMeanEitherOfThem() throws Exception {
        createCodedPerson("Satu", "MALE", "O_POSITIVE", "KTP");
        createCodedPerson("Dua", "FEMALE", "O_NEGATIVE", "KTP");
        createCodedPerson("Tiga", "FEMALE", "AB_NEGATIVE", "KTP");
        createCodedPerson("Empat", "MALE", "A_POSITIVE", "KTP");

        mockMvc.perform(asOwner(get("/person")).param("bloodType", "O_POSITIVE", "O_NEGATIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // One value still behaves exactly as it did before.
        mockMvc.perform(asOwner(get("/person")).param("bloodType", "AB_NEGATIVE"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Tiga"));
    }

    /**
     * Within one filter the values mean either; against another filter, and
     * against the search, they still mean both.
     */
    @Test
    void severalBloodTypesStillNarrowAgainstEverythingElse() throws Exception {
        createCodedPerson("Satu", "MALE", "O_POSITIVE", "KTP");
        createCodedPerson("Dua", "FEMALE", "O_NEGATIVE", "KTP");
        createCodedPerson("Tiga", "FEMALE", "O_POSITIVE", "PASSPORT");

        mockMvc.perform(asOwner(get("/person"))
                        .param("bloodType", "O_POSITIVE", "O_NEGATIVE")
                        .param("gender", "FEMALE"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(asOwner(get("/person"))
                        .param("bloodType", "O_POSITIVE", "O_NEGATIVE")
                        .param("gender", "FEMALE")
                        .param("identityDocumentType", "PASSPORT"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Tiga"));

        mockMvc.perform(asOwner(get("/person"))
                        .param("q", "Female")
                        .param("bloodType", "O_NEGATIVE"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Dua"));
    }

    /**
     * "KTP or Kartu Keluarga" is a question too, so identity document takes
     * several the same way blood type does.
     */
    @Test
    void severalIdentityDocumentsMeanEitherOfThem() throws Exception {
        createCodedPerson("Satu", "MALE", "O_POSITIVE", "KTP");
        createCodedPerson("Dua", "FEMALE", "O_POSITIVE", "KARTU_KELUARGA");
        createCodedPerson("Tiga", "FEMALE", "AB_NEGATIVE", "PASSPORT");
        createCodedPerson("Empat", "MALE", "A_POSITIVE", "KITAS");

        mockMvc.perform(asOwner(get("/person"))
                        .param("identityDocumentType", "KTP", "KARTU_KELUARGA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(asOwner(get("/person")).param("identityDocumentType", "KITAS"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Empat"));
    }

    /**
     * Two multi-valued filters at once: either within each, both across them.
     * This is the combination most easily got backwards.
     */
    @Test
    void twoMultiValuedFiltersStillMeanBothAcross() throws Exception {
        createCodedPerson("Satu", "MALE", "O_POSITIVE", "KTP");
        createCodedPerson("Dua", "FEMALE", "O_NEGATIVE", "PASSPORT");
        createCodedPerson("Tiga", "FEMALE", "AB_NEGATIVE", "KTP");
        createCodedPerson("Empat", "MALE", "O_POSITIVE", "KITAS");

        mockMvc.perform(asOwner(get("/person"))
                        .param("bloodType", "O_POSITIVE", "O_NEGATIVE")
                        .param("identityDocumentType", "KTP", "PASSPORT"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(asOwner(get("/person"))
                        .param("bloodType", "O_POSITIVE", "O_NEGATIVE")
                        .param("identityDocumentType", "KTP", "PASSPORT")
                        .param("gender", "FEMALE"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Dua"));
    }

    /**
     * "Single or widowed" is a question too.
     */
    @Test
    void severalMaritalStatusesMeanEitherOfThem() throws Exception {
        createMarried("Satu", "MALE", "SINGLE");
        createMarried("Dua", "FEMALE", "WIDOWED");
        createMarried("Tiga", "FEMALE", "MARRIED");
        createMarried("Empat", "MALE", "DIVORCED");

        mockMvc.perform(asOwner(get("/person")).param("maritalStatus", "SINGLE", "WIDOWED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(asOwner(get("/person"))
                        .param("maritalStatus", "SINGLE", "WIDOWED")
                        .param("gender", "FEMALE"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Dua"));
    }

    @Test
    void severalGendersMeanEitherOfThem() throws Exception {
        createCodedPerson("Satu", "MALE", "O_POSITIVE", "KTP");
        createCodedPerson("Dua", "FEMALE", "AB_NEGATIVE", "PASSPORT");
        createCodedPerson("Tiga", "FEMALE", "O_POSITIVE", "KITAS");

        mockMvc.perform(asOwner(get("/person")).param("gender", "MALE", "FEMALE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(asOwner(get("/person")).param("gender", "FEMALE"))
                .andExpect(jsonPath("$.totalElements").value(2));

        // Still narrows against another filter rather than widening it.
        mockMvc.perform(asOwner(get("/person"))
                        .param("gender", "MALE", "FEMALE")
                        .param("bloodType", "O_POSITIVE"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    /**
     * Ticking every box is not the same as ticking none, and the difference is
     * whoever has nothing recorded. "Gender is male or female" honestly excludes
     * the person whose gender was never asked; an untouched filter keeps them.
     * <p>
     * It surprises people, so it is pinned here rather than left to be
     * rediscovered by somebody comparing two counts.
     */
    @Test
    void namingEveryValueIsNotTheSameAsNamingNone() throws Exception {
        createCodedPerson("Satu", "MALE", "O_POSITIVE", "KTP");
        createCodedPerson("Dua", "FEMALE", "AB_NEGATIVE", "PASSPORT");
        createPerson("Tanpa", "Gender", "tanpa@probe.test", "0899");

        mockMvc.perform(asOwner(get("/person")).param("gender", "MALE", "FEMALE"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(asOwner(get("/person")))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    /**
     * All four at once, which is now the whole filter set. Either within each,
     * both across them.
     */
    @Test
    void everyFilterAtOnceStillMeansBothAcross() throws Exception {
        createCodedPerson("Satu", "MALE", "O_POSITIVE", "KTP");
        createCodedPerson("Dua", "FEMALE", "O_NEGATIVE", "PASSPORT");
        createCodedPerson("Tiga", "FEMALE", "O_POSITIVE", "KTP");

        mockMvc.perform(asOwner(get("/person"))
                        .param("gender", "FEMALE")
                        .param("bloodType", "O_POSITIVE", "O_NEGATIVE")
                        .param("identityDocumentType", "KTP", "KITAS"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Tiga"));
    }

    /**
     * Repeating a value, or padding it with blanks, is somebody poking at the
     * URL rather than a different question.
     */
    @Test
    void repeatedAndBlankBloodTypesAreTidiedAway() throws Exception {
        createCodedPerson("Satu", "MALE", "O_POSITIVE", "KTP");
        createCodedPerson("Dua", "FEMALE", "AB_NEGATIVE", "KTP");

        mockMvc.perform(asOwner(get("/person"))
                        .param("bloodType", "O_POSITIVE", "o_positive", "  ", "O_POSITIVE"))
                .andExpect(jsonPath("$.totalElements").value(1));

        // All blank is no filter at all, not an empty result.
        mockMvc.perform(asOwner(get("/person")).param("bloodType", "", "  "))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    /**
     * A blank filter is "any", not "none": an empty dropdown must not empty the
     * list. An unknown code matches nothing, which is the honest answer.
     */
    @Test
    void aBlankFilterIsAnyAndAnUnknownOneIsNone() throws Exception {
        createCodedPerson("Satu", "MALE", "O_POSITIVE", "KTP");
        createCodedPerson("Dua", "FEMALE", "AB_NEGATIVE", "PASSPORT");

        mockMvc.perform(asOwner(get("/person")).param("gender", "").param("bloodType", "  "))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(asOwner(get("/person")).param("gender", "ROBOT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // A filter takes the code, so a label typed into one matches nothing —
        // wherever the two actually differ. (For a single-word list like GENDER
        // they coincide once upper-cased, which is luck rather than design.)
        mockMvc.perform(asOwner(get("/person")).param("bloodType", "O+"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private MockMultipartFile personPart(String firstName) throws Exception {
        return new MockMultipartFile("person", "person", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(Map.of("firstName", firstName, "lastName", "Probe")));
    }

    private MockMultipartFile photoPart(String bytes) {
        given(storageService.store(any(), anyString())).willReturn("persons/probe.png");
        given(storageService.urlOf("persons/probe.png"))
                .willReturn("https://cdn.example.test/persons/probe.png?X-Amz-Signature=abc");
        return new MockMultipartFile("photo", "me.png", MediaType.IMAGE_PNG_VALUE, bytes.getBytes());
    }

    /**
     * A photo arrives with the record rather than in a second call, so there is
     * no window where the person exists without it.
     */
    @Test
    void aPersonCanBeCreatedWithAPhoto() throws Exception {
        String body = mockMvc.perform(asOwnerMultipart(multipart("/person"))
                        .file(personPart("Berfoto")).file(photoPart("png-bytes")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Berfoto"))
                .andExpect(jsonPath("$.photoUrl").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // The key is storage; what leaves the API is a URL and nothing else.
        assertThat(body).doesNotContain("photoKey");

        long id = objectMapper.readTree(body).get("id").asLong();
        mockMvc.perform(asOwner(get("/person/" + id)))
                .andExpect(jsonPath("$.photoUrl").isNotEmpty());
    }

    /**
     * Somebody with no photo must come back as null rather than as something
     * that renders a broken image.
     */
    @Test
    void aPersonWithoutAPhotoHasNoUrl() throws Exception {
        createPerson("Tanpa", "Foto", "tanpa@probe.test", "0899");

        mockMvc.perform(asOwner(get("/person")))
                .andExpect(jsonPath("$.content[0].photoUrl").doesNotExist());
    }

    /**
     * An edit that leaves the file input empty must not wipe the photo — that is
     * what the omitted part means on the organization form too.
     */
    @Test
    void anEditWithoutAPhotoPartKeepsTheCurrentOne() throws Exception {
        String body = mockMvc.perform(asOwnerMultipart(multipart("/person"))
                        .file(personPart("Tetap")).file(photoPart("png-bytes")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(asOwner(put("/person/" + id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Tetap\",\"lastName\":\"Diubah\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Diubah"))
                .andExpect(jsonPath("$.photoUrl").isNotEmpty());
    }

    /**
     * Replacing a photo was possible; having none again was not. Somebody who
     * uploaded the wrong face had no way back.
     */
    @Test
    void aPhotoCanBeRemoved() throws Exception {
        String body = mockMvc.perform(asOwnerMultipart(multipart("/person"))
                        .file(personPart("Dihapus")).file(photoPart("png-bytes")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoUrl").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(asOwnerMultipart(multipart("/person/" + id))
                        .file(personPart("Dihapus"))
                        .param("removePhoto", "true")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").doesNotExist());

        // The object goes with it rather than lingering unreferenced.
        verify(storageService).delete("persons/probe.png");
    }

    /**
     * Asking to remove one while uploading another is a contradiction. The
     * upload wins — choosing a file says more than ticking a box — and the
     * record must not end up with neither.
     */
    @Test
    void anUploadWinsOverTheRemovalFlag() throws Exception {
        String body = mockMvc.perform(asOwnerMultipart(multipart("/person"))
                        .file(personPart("Bimbang")).file(photoPart("png-bytes")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(asOwnerMultipart(multipart("/person/" + id))
                        .file(personPart("Bimbang"))
                        .file(photoPart("other-bytes"))
                        .param("removePhoto", "true")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").isNotEmpty());
    }

    /**
     * The flag on somebody who has no photo is not an error, it is nothing.
     */
    @Test
    void removingWhenThereIsNoPhotoDoesNothing() throws Exception {
        createPerson("Tanpa", "Foto", "tanpa@probe.test", "0899");
        long id = objectMapper.readTree(mockMvc.perform(asOwner(get("/person")))
                .andReturn().getResponse().getContentAsString()).get("content").get(0).get("id").asLong();

        mockMvc.perform(asOwnerMultipart(multipart("/person/" + id))
                        .file(personPart("Tanpa"))
                        .param("removePhoto", "true")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").doesNotExist());
        verify(storageService, never()).delete(any());
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
