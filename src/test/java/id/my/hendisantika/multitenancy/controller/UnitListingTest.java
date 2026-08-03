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
import id.my.hendisantika.multitenancy.service.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    /**
     * Mocked rather than reaching MinIO, for the same reason the person listing
     * mocks it: these tests are about how a photo is wired through the
     * controller and the service, not about the bucket, and CI starts without
     * one.
     */
    @MockitoBean
    private StorageService storageService;

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

    private MockMultipartHttpServletRequestBuilder asOwnerMultipart(
            MockMultipartHttpServletRequestBuilder request) {
        request.header("Authorization", "Bearer " + ownerToken)
                .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG);
        return request;
    }

    private MockMultipartHttpServletRequestBuilder asPut(MockMultipartHttpServletRequestBuilder request) {
        return (MockMultipartHttpServletRequestBuilder) request.with(raw -> {
            raw.setMethod("PUT");
            return raw;
        });
    }

    private MockMultipartFile unitPart(String name) {
        return new MockMultipartFile("organization", "organization", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(Map.of("name", name)));
    }

    private MockMultipartFile photoPart(String bytes) {
        given(storageService.store(any(), anyString())).willReturn("units/probe.png");
        given(storageService.urlOf("units/probe.png"))
                .willReturn("https://cdn.example.test/units/probe.png?X-Amz-Signature=abc");
        return new MockMultipartFile("photo", "unit.png", MediaType.IMAGE_PNG_VALUE, bytes.getBytes());
    }

    private long createUnitWithPhoto(String name) throws Exception {
        String body = mockMvc.perform(asOwnerMultipart(multipart("/organization"))
                        .file(unitPart(name)).file(photoPart("png-bytes")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoUrl").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    /**
     * A photo arrives with the unit rather than in a second call, so there is no
     * window where the unit exists without it.
     */
    @Test
    void aUnitCanBeCreatedWithAPhoto() throws Exception {
        String body = mockMvc.perform(asOwnerMultipart(multipart("/organization"))
                        .file(unitPart("Cabang Berfoto")).file(photoPart("png-bytes")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cabang Berfoto"))
                .andExpect(jsonPath("$.photoUrl").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // The key is storage; what leaves the API is a URL and nothing else.
        assertThat(body).doesNotContain("photoKey");

        long id = objectMapper.readTree(body).get("id").asLong();
        mockMvc.perform(withTenant(get("/organization/" + id), ownerToken))
                .andExpect(jsonPath("$.photoUrl").isNotEmpty());
    }

    /**
     * A unit with no photo must come back as null rather than as something that
     * renders a broken image.
     */
    @Test
    void aUnitWithoutAPhotoHasNoUrl() throws Exception {
        createUnit("Cabang Polos", "Jalan Polos", "polos@probe.test");

        mockMvc.perform(withTenant(get("/organization"), ownerToken))
                .andExpect(jsonPath("$.content[0].photoUrl").doesNotExist());
    }

    /**
     * An edit that leaves the file input empty must not wipe the photo.
     */
    @Test
    void anEditWithoutAPhotoPartKeepsTheCurrentOne() throws Exception {
        long id = createUnitWithPhoto("Cabang Tetap");

        mockMvc.perform(withTenant(put("/organization/" + id), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cabang Diubah\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cabang Diubah"))
                .andExpect(jsonPath("$.photoUrl").isNotEmpty());
    }

    @Test
    void aUnitPhotoCanBeRemoved() throws Exception {
        long id = createUnitWithPhoto("Cabang Dihapus");

        mockMvc.perform(asPut(asOwnerMultipart(multipart("/organization/" + id)))
                        .file(unitPart("Cabang Dihapus"))
                        .param("removePhoto", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").doesNotExist());

        // The object goes with it rather than lingering unreferenced.
        verify(storageService).delete("units/probe.png");
    }

    /**
     * Asking to remove one while uploading another is a contradiction. The
     * upload wins, and the unit must not end up with neither.
     */
    @Test
    void anUploadWinsOverTheRemovalFlagOnAUnit() throws Exception {
        long id = createUnitWithPhoto("Cabang Bimbang");

        mockMvc.perform(asPut(asOwnerMultipart(multipart("/organization/" + id)))
                        .file(unitPart("Cabang Bimbang"))
                        .file(photoPart("other-bytes"))
                        .param("removePhoto", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").isNotEmpty());
    }

    /**
     * Otherwise the bucket keeps a picture of a unit that no longer exists, and
     * nothing left points at it to say so.
     */
    @Test
    void deletingAUnitTakesItsPhotoWithIt() throws Exception {
        long id = createUnitWithPhoto("Cabang Ditutup");

        mockMvc.perform(withTenant(delete("/organization/" + id), ownerToken))
                .andExpect(status().isNoContent());

        verify(storageService).delete("units/probe.png");
    }

    /**
     * Writing units is the owner's, and attaching a photo is writing: the
     * multipart way in must not be a way around that.
     */
    @Test
    void aMemberCannotAttachAPhotoEither() throws Exception {
        mockMvc.perform(multipart("/organization")
                        .file(unitPart("Cabang Terlarang")).file(photoPart("png-bytes"))
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isForbidden());
    }

    private void createCodedUnit(String name, String unitType, String status, String province) throws Exception {
        Map<String, String> unit = new LinkedHashMap<>();
        unit.put("name", name);
        unit.put("unitType", unitType);
        unit.put("operatingStatus", status);
        unit.put("province", province);
        mockMvc.perform(withTenant(post("/organization"), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unit)))
                .andExpect(status().isCreated());
    }

    /**
     * The record stores BRANCH_CLINIC and DKI_JAKARTA. Nobody types that, so the
     * search has to reach the label a clinic actually reads.
     */
    @Test
    void aSearchReachesTheCodedFieldsByTheirLabel() throws Exception {
        createCodedUnit("Satu", "MAIN_CLINIC", "OPEN", "DKI_JAKARTA");
        createCodedUnit("Dua", "BRANCH_CLINIC", "TEMPORARILY_CLOSED", "BALI");
        createCodedUnit("Tiga", "PHARMACY", "OPEN", "JAWA_BARAT");

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "Bali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Dua"));

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "branch clinic"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Dua"));

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "pharmacy"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Tiga"));

        // Case does not matter, here as everywhere else.
        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "JAWA BARAT"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    /**
     * A label matches by substring, so one word can pick out a whole group.
     */
    @Test
    void aPartialLabelMatchesEveryUnitThatShareIt() throws Exception {
        createCodedUnit("Satu", "MAIN_CLINIC", "OPEN", "DKI_JAKARTA");
        createCodedUnit("Dua", "BRANCH_CLINIC", "OPEN", "BALI");
        createCodedUnit("Tiga", "PHARMACY", "OPEN", "BALI");

        // "clinic" is in both Main clinic and Branch clinic.
        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "clinic"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "open"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    /**
     * The search is over labels, which are what a person reads. The stored code
     * is never shown, so nobody is searching for one, and matching it would
     * make a typed underscore behave unlike everywhere else.
     */
    @Test
    void theStoredCodeIsNotWhatIsSearched() throws Exception {
        createCodedUnit("Dua", "BRANCH_CLINIC", "OPEN", "BALI");

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "BRANCH_CLINIC"))
                .andExpect(jsonPath("$.totalElements").value(0));

        // The label it stands for does find it.
        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "Branch clinic"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    /**
     * A term that matches no label must not quietly match everything, which is
     * what an empty {@code in} clause would do if it were written carelessly.
     */
    @Test
    void aTermMatchingNoLabelStillFindsNothing() throws Exception {
        createCodedUnit("Satu", "MAIN_CLINIC", "OPEN", "DKI_JAKARTA");
        createCodedUnit("Dua", "BRANCH_CLINIC", "OPEN", "BALI");

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /**
     * A unit with nothing coded is found by its name as before, and is not swept
     * up by a search for a label it does not carry.
     */
    @Test
    void aUnitWithNoCodesIsUnaffected() throws Exception {
        createUnit("Cabang Polos", "Jalan Kosong", "polos@probe.test");
        createCodedUnit("Cabang Bali", "BRANCH_CLINIC", "OPEN", "BALI");

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "polos"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Cabang Polos"));

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "Bali"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Cabang Bali"));
    }

    /**
     * A wildcard is still a character somebody typed, not an instruction — the
     * label side matches in Java, so it never had any to honour.
     */
    @Test
    void wildcardsStayLiteralAcrossBothSidesOfTheSearch() throws Exception {
        createCodedUnit("Cabang Bali", "BRANCH_CLINIC", "OPEN", "BALI");

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "%"))
                .andExpect(jsonPath("$.totalElements").value(0));

        // The trap this caught: codes contain underscores, so matching them
        // would have made a bare "_" find every unit with a two-word code.
        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "_"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /**
     * A filter narrows on one field, exactly, where a search widens across many
     * loosely. So it takes the code, not the label.
     */
    @Test
    void aFilterNarrowsOnOneFieldExactly() throws Exception {
        createCodedUnit("Satu", "MAIN_CLINIC", "OPEN", "DKI_JAKARTA");
        createCodedUnit("Dua", "BRANCH_CLINIC", "TEMPORARILY_CLOSED", "BALI");
        createCodedUnit("Tiga", "BRANCH_CLINIC", "OPEN", "BALI");

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("province", "BALI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("unitType", "MAIN_CLINIC"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Satu"));

        // Case does not matter; the stored code is the canonical one.
        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("province", "bali"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    /**
     * Two filters mean both, not either — the opposite of how the search
     * combines its fields.
     */
    @Test
    void filtersCombineWithEachOther() throws Exception {
        createCodedUnit("Satu", "MAIN_CLINIC", "OPEN", "DKI_JAKARTA");
        createCodedUnit("Dua", "BRANCH_CLINIC", "TEMPORARILY_CLOSED", "BALI");
        createCodedUnit("Tiga", "BRANCH_CLINIC", "OPEN", "BALI");

        mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("province", "BALI")
                        .param("operatingStatus", "OPEN"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Tiga"));
    }

    /**
     * A search widens, a filter narrows, and together they mean both. Getting
     * this wrong the other way would quietly return more than was asked for.
     */
    @Test
    void aFilterNarrowsTheSearchRatherThanWideningIt() throws Exception {
        createCodedUnit("Cabang Bali", "BRANCH_CLINIC", "OPEN", "BALI");
        createCodedUnit("Cabang Jakarta", "BRANCH_CLINIC", "OPEN", "DKI_JAKARTA");
        createCodedUnit("Apotek Bali", "PHARMACY", "OPEN", "BALI");

        // The search alone finds all three: two by name, one by province label.
        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "Bali"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("q", "Cabang"))
                .andExpect(jsonPath("$.totalElements").value(2));

        // With a filter it is only the ones that are both.
        mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("q", "Cabang")
                        .param("province", "BALI"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Cabang Bali"));
    }

    /**
     * Filtering is not searching: a filter takes the stored code, so a label
     * typed into it matches nothing rather than quietly matching everything.
     */
    @Test
    void anUnknownFilterCodeMatchesNothing() throws Exception {
        createCodedUnit("Satu", "MAIN_CLINIC", "OPEN", "DKI_JAKARTA");

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("province", "ATLANTIS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("unitType", "Main clinic"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /**
     * There are 38 provinces, so "the Bali and Jawa Barat branches" is a real
     * question. Several values in one filter mean either of them.
     */
    @Test
    void severalProvincesMeanEitherOfThem() throws Exception {
        createCodedUnit("Bali", "BRANCH_CLINIC", "OPEN", "BALI");
        createCodedUnit("Bandung", "BRANCH_CLINIC", "OPEN", "JAWA_BARAT");
        createCodedUnit("Jakarta", "BRANCH_CLINIC", "OPEN", "DKI_JAKARTA");
        createCodedUnit("Medan", "BRANCH_CLINIC", "OPEN", "SUMATERA_UTARA");

        mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("province", "BALI", "JAWA_BARAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // One value still behaves exactly as it did before.
        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("province", "BALI"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Bali"));
    }

    /**
     * Within one filter the values mean either; against another filter they
     * still mean both. Getting that wrong would widen rather than narrow.
     */
    @Test
    void severalProvincesStillNarrowAgainstTheOtherFilters() throws Exception {
        createCodedUnit("Bali terbuka", "BRANCH_CLINIC", "OPEN", "BALI");
        createCodedUnit("Bali tutup", "BRANCH_CLINIC", "TEMPORARILY_CLOSED", "BALI");
        createCodedUnit("Bandung terbuka", "PHARMACY", "OPEN", "JAWA_BARAT");
        createCodedUnit("Jakarta terbuka", "BRANCH_CLINIC", "OPEN", "DKI_JAKARTA");

        mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("province", "BALI", "JAWA_BARAT")
                        .param("operatingStatus", "OPEN"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("province", "BALI", "JAWA_BARAT")
                        .param("operatingStatus", "OPEN")
                        .param("unitType", "BRANCH_CLINIC"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Bali terbuka"));

        // And with a search on top, which narrows again rather than widening.
        mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("q", "terbuka")
                        .param("province", "BALI", "JAWA_BARAT"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    /**
     * "The clinics and the pharmacies" is a question too, so unit type takes
     * several the same way province does.
     */
    @Test
    void severalUnitTypesMeanEitherOfThem() throws Exception {
        createCodedUnit("Satu", "MAIN_CLINIC", "OPEN", "BALI");
        createCodedUnit("Dua", "BRANCH_CLINIC", "OPEN", "BALI");
        createCodedUnit("Tiga", "PHARMACY", "OPEN", "BALI");
        createCodedUnit("Empat", "LABORATORY", "OPEN", "BALI");

        mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("unitType", "MAIN_CLINIC", "BRANCH_CLINIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("unitType", "PHARMACY"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Tiga"));
    }

    /**
     * Two multi-valued filters at once: either within each, both across them.
     * This is the combination most likely to have been got backwards.
     */
    @Test
    void twoMultiValuedFiltersStillMeanBothAcross() throws Exception {
        createCodedUnit("Klinik Bali", "BRANCH_CLINIC", "OPEN", "BALI");
        createCodedUnit("Apotek Bali", "PHARMACY", "OPEN", "BALI");
        createCodedUnit("Lab Bali", "LABORATORY", "OPEN", "BALI");
        createCodedUnit("Klinik Bandung", "BRANCH_CLINIC", "OPEN", "JAWA_BARAT");
        createCodedUnit("Klinik Medan", "BRANCH_CLINIC", "OPEN", "SUMATERA_UTARA");

        mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("unitType", "BRANCH_CLINIC", "PHARMACY")
                        .param("province", "BALI", "JAWA_BARAT"))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("unitType", "BRANCH_CLINIC", "PHARMACY")
                        .param("province", "BALI", "JAWA_BARAT")
                        .param("operatingStatus", "OPEN")
                        .param("q", "Apotek"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Apotek Bali"));
    }

    /**
     * "Closed for now or for good" is a question too, and it is the last of the
     * unit filters to take several — so this is also the case where all three
     * are multi-valued at once.
     */
    @Test
    void severalOperatingStatusesMeanEitherOfThem() throws Exception {
        createCodedUnit("Buka", "BRANCH_CLINIC", "OPEN", "BALI");
        createCodedUnit("Tutup sementara", "BRANCH_CLINIC", "TEMPORARILY_CLOSED", "BALI");
        createCodedUnit("Tutup permanen", "PHARMACY", "PERMANENTLY_CLOSED", "JAWA_BARAT");
        createCodedUnit("Segera buka", "PHARMACY", "OPENING_SOON", "DKI_JAKARTA");

        mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("operatingStatus", "TEMPORARILY_CLOSED", "PERMANENTLY_CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // All three multi-valued at once: either within each, both across them.
        mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("unitType", "BRANCH_CLINIC", "PHARMACY")
                        .param("operatingStatus", "TEMPORARILY_CLOSED", "PERMANENTLY_CLOSED")
                        .param("province", "BALI", "DKI_JAKARTA"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Tutup sementara"));
    }

    /**
     * Repeating a value, or padding it with blanks, is somebody poking at the
     * URL rather than a different question.
     */
    @Test
    void repeatedAndBlankProvincesAreTidiedAway() throws Exception {
        createCodedUnit("Bali", "BRANCH_CLINIC", "OPEN", "BALI");
        createCodedUnit("Jakarta", "BRANCH_CLINIC", "OPEN", "DKI_JAKARTA");

        mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("province", "BALI", "bali", "  ", "BALI"))
                .andExpect(jsonPath("$.totalElements").value(1));

        // All blank is no filter at all, not an empty result.
        mockMvc.perform(withTenant(get("/organization"), ownerToken).param("province", "", "  "))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    /**
     * A blank filter is "any", not "none": an empty dropdown must not empty the
     * list.
     */
    @Test
    void aBlankFilterIsAny() throws Exception {
        createUnits(3);

        mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("province", "")
                        .param("unitType", "  "))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    /**
     * A filtered list still pages, and the count is of the filtered set rather
     * than of everything.
     */
    @Test
    void aFilteredListStillPages() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createCodedUnit("Bali " + i, "BRANCH_CLINIC", "OPEN", "BALI");
        }
        createCodedUnit("Jakarta", "BRANCH_CLINIC", "OPEN", "DKI_JAKARTA");

        mockMvc.perform(withTenant(get("/organization"), ownerToken)
                        .param("province", "BALI").param("size", "2"))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content.length()").value(2));
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
