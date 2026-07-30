package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.config.TenantSubdomainInterceptor;
import id.my.hendisantika.multitenancy.entity.central.OrgStructure;
import id.my.hendisantika.multitenancy.entity.central.PracticeSpeciality;
import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.entity.central.TenantRole;
import id.my.hendisantika.multitenancy.repository.central.AccountRepository;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
import id.my.hendisantika.multitenancy.repository.central.UserTenantRepository;
import id.my.hendisantika.multitenancy.service.TenantProvisioningService;
import id.my.hendisantika.multitenancy.service.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The organization registration form, the owner adding people to it, and the
 * line between what an owner may do and what a member may do.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.28
 */
@SpringBootTest
class OrganizationRegistrationTest {

    private static final String OWNER_EMAIL = "org.owner.probe@example.test";
    private static final String MEMBER_EMAIL = "org.member.probe@example.test";
    private static final String PASSWORD = "s3cret-password";
    private static final String BUSINESS_NAME = "Klinik Sehat Probe";
    private static final String SLUG = "kliniksehatprobe";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserTenantRepository userTenantRepository;

    @Autowired
    private TenantRegistrationRepository tenantRegistrationRepository;

    @Autowired
    private TenantProvisioningService tenantProvisioningService;

    @MockitoBean
    private StorageService storageService;

    private MockMvc mockMvc;

    private MockMvc mvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        }
        return mockMvc;
    }

    @AfterEach
    void cleanUp() {
        tenantRegistrationRepository.findBySlug(SLUG)
                .ifPresent(tenant -> tenantProvisioningService.deprovision(SLUG));
        List.of(OWNER_EMAIL, MEMBER_EMAIL).forEach(email ->
                accountRepository.findByEmailIgnoreCase(email).ifPresent(account -> {
                    userTenantRepository.deleteAll(userTenantRepository.findAllByAccountId(account.getId()));
                    accountRepository.delete(account);
                }));
    }

    private String signUp(String email) throws Exception {
        given(storageService.store(any(), anyString())).willReturn("organizations/probe.png");
        given(storageService.urlOf(anyString())).willReturn("https://cdn.example.test/organizations/probe.png");

        MockMultipartFile account = new MockMultipartFile("account", "account", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(
                        new AuthController.SignUpRequest(email, "+62 812 3456 7890", PASSWORD)));
        mvc().perform(multipart("/api/auth/signup").file(account)).andExpect(status().isCreated());
        return login(email);
    }

    private String login(String email) throws Exception {
        String body = mvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private MockMultipartFile organizationPart() {
        return new MockMultipartFile("organization", "organization", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(
                        new OrganizationRegistrationController.RegisterOrganizationRequest(
                                BUSINESS_NAME, "clinic@example.test", "Hendi", "Santika",
                                "Practice Manager", "+62 811 2233 4455",
                                OrgStructure.MULTI_LOCATION_CLINIC,
                                PracticeSpeciality.AESTHETIC_AND_DERMA)));
    }

    private String registerOrganization(String ownerToken) throws Exception {
        MockMultipartFile photo = new MockMultipartFile("photo", "logo.png", MediaType.IMAGE_PNG_VALUE,
                "not-really-a-png".getBytes());
        mvc().perform(multipart("/api/organizations")
                        .file(organizationPart()).file(photo)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated());
        return login(OWNER_EMAIL);
    }

    @Test
    void storesTheWholeRegistrationForm() throws Exception {
        String token = signUp(OWNER_EMAIL);

        mvc().perform(multipart("/api/organizations")
                        .file(organizationPart())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value(SLUG))
                .andExpect(jsonPath("$.businessName").value(BUSINESS_NAME))
                .andExpect(jsonPath("$.businessEmail").value("clinic@example.test"))
                .andExpect(jsonPath("$.jobTitle").value("Practice Manager"))
                .andExpect(jsonPath("$.orgStructure").value("MULTI_LOCATION_CLINIC"))
                .andExpect(jsonPath("$.practiceSpeciality").value("AESTHETIC_AND_DERMA"))
                .andExpect(jsonPath("$.subdomain").value(SLUG + ".mhdc.co.id"));

        TenantRegistration tenant = tenantRegistrationRepository.findBySlug(SLUG).orElseThrow();
        assertThat(tenant.getContactFirstName()).isEqualTo("Hendi");
        assertThat(tenant.getContactLastName()).isEqualTo("Santika");
        assertThat(tenant.getPhoneNumber()).isEqualTo("+62 811 2233 4455");
        assertThat(tenant.getOrgStructure()).isEqualTo(OrgStructure.MULTI_LOCATION_CLINIC);
        assertThat(tenant.getPracticeSpeciality()).isEqualTo(PracticeSpeciality.AESTHETIC_AND_DERMA);
    }

    @Test
    void rejectsAFormThatIsMissingTheStructureOrSpeciality() throws Exception {
        String token = signUp(OWNER_EMAIL);

        MockMultipartFile incomplete = new MockMultipartFile("organization", "organization",
                MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(Map.of(
                "businessName", BUSINESS_NAME,
                "businessEmail", "clinic@example.test",
                "contactFirstName", "Hendi",
                "contactLastName", "Santika",
                "jobTitle", "Practice Manager",
                "phoneNumber", "+62 811 2233 4455")));

        mvc().perform(multipart("/api/organizations")
                        .file(incomplete)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        assertThat(tenantRegistrationRepository.findBySlug(SLUG)).isEmpty();
    }

    @Test
    void ownerAddsAMemberWhoCanThenReachTheTenant() throws Exception {
        String ownerToken = registerOrganization(signUp(OWNER_EMAIL));

        mvc().perform(post("/api/organizations/" + SLUG + "/users")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrganizationRegistrationController.AddMemberRequest(
                                        MEMBER_EMAIL, "+62 813 0000 1111", PASSWORD, TenantRole.MEMBER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(MEMBER_EMAIL))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        // The member the owner just created can log in and reach the tenant.
        String memberToken = login(MEMBER_EMAIL);
        mvc().perform(get("/organization/1")
                        .header("Authorization", "Bearer " + memberToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isOk());
    }

    @Test
    void memberCannotAddFurtherMembers() throws Exception {
        String ownerToken = registerOrganization(signUp(OWNER_EMAIL));
        mvc().perform(post("/api/organizations/" + SLUG + "/users")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrganizationRegistrationController.AddMemberRequest(
                                        MEMBER_EMAIL, "+62 813 0000 1111", PASSWORD, TenantRole.MEMBER))))
                .andExpect(status().isCreated());

        String memberToken = login(MEMBER_EMAIL);
        mvc().perform(post("/api/organizations/" + SLUG + "/users")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrganizationRegistrationController.AddMemberRequest(
                                        "someone.else@example.test", "+62 813 0000 2222", PASSWORD,
                                        TenantRole.MEMBER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listingIsScopedToTheCallersOwnOrganizations() throws Exception {
        String ownerToken = registerOrganization(signUp(OWNER_EMAIL));

        mvc().perform(get("/api/organizations").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value(SLUG));

        // An outsider sees nothing, and cannot read the organization directly.
        String outsiderToken = signUp(MEMBER_EMAIL);
        mvc().perform(get("/api/organizations").header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc().perform(get("/api/organizations/" + SLUG).header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void theOwnerCannotBeRemovedFromTheirOwnOrganization() throws Exception {
        String ownerToken = registerOrganization(signUp(OWNER_EMAIL));
        Long ownerId = accountRepository.findByEmailIgnoreCase(OWNER_EMAIL).orElseThrow().getId();

        mvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/organizations/" + SLUG + "/users/" + ownerId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());

        assertThat(userTenantRepository.existsByAccountIdAndTenantSlug(ownerId, SLUG)).isTrue();
    }
}
