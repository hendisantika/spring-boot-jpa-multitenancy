package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.config.TenantSubdomainInterceptor;
import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.OrgStructure;
import id.my.hendisantika.multitenancy.entity.central.PracticeSpeciality;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Walks the whole parent login: sign up, log in, register an organization and
 * discover that the token only opens the tenants it was granted.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@SpringBootTest
class AuthFlowTest {

    private static final String EMAIL = "owner.probe@example.test";
    private static final String PASSWORD = "s3cret-password";
    private static final String ORGANIZATION = "Auth Probe Clinic";
    private static final String SLUG = "authprobeclinic";

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

    /**
     * The bucket is not part of this test; storing a photo is verified separately
     * against a mocked S3 client.
     */
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
        accountRepository.findByEmailIgnoreCase(EMAIL).ifPresent(account -> {
            userTenantRepository.deleteAll(userTenantRepository.findAllByAccountId(account.getId()));
            accountRepository.delete(account);
        });
    }

    private MockMultipartFile organizationPart() {
        return new MockMultipartFile("organization", "organization", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new OrganizationRegistrationController.RegisterOrganizationRequest(
                        ORGANIZATION, "clinic@example.test", "Hendi", "Santika", "Owner",
                        "+62 812 3456 7890", OrgStructure.SINGLE_LOCATION_CLINIC,
                        PracticeSpeciality.GENERAL_PRACTICE)));
    }

    private String signUpAndLogin() throws Exception {
        given(storageService.store(any(), anyString())).willReturn("accounts/probe.jpg");
        given(storageService.urlOf(anyString())).willReturn("https://cdn.example.test/accounts/probe.jpg");

        MockMultipartFile account = new MockMultipartFile("account", "account", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new AuthController.SignUpRequest(EMAIL, "+62 812 3456 7890", PASSWORD)));
        MockMultipartFile photo = new MockMultipartFile("photo", "me.jpg", MediaType.IMAGE_JPEG_VALUE,
                "not-really-a-jpeg".getBytes());

        mvc().perform(multipart("/api/auth/signup").file(account).file(photo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.photoUrl").value("https://cdn.example.test/accounts/probe.jpg"));

        String body = mvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.LoginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private MockMultipartHttpServletRequestBuilder putPhoto(String token) {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/auth/me/photo");
        request.header("Authorization", "Bearer " + token).with(r -> {
            r.setMethod("PUT");
            return r;
        });
        return request;
    }

    /**
     * Signup could set a photo and nothing could change it afterwards, so an
     * account was stuck with whichever face it registered with.
     */
    @Test
    void theAccountPhotoCanBeReplaced() throws Exception {
        String token = signUpAndLogin();
        given(storageService.store(any(), anyString())).willReturn("accounts/second.jpg");
        given(storageService.urlOf("accounts/second.jpg"))
                .willReturn("https://cdn.example.test/accounts/second.jpg");

        mvc().perform(putPhoto(token)
                        .file(new MockMultipartFile("photo", "new.jpg", MediaType.IMAGE_JPEG_VALUE,
                                "another".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("https://cdn.example.test/accounts/second.jpg"));

        // The one it replaced goes with it rather than lingering unreferenced.
        verify(storageService).delete("accounts/probe.jpg");
        assertThat(accountRepository.findByEmailIgnoreCase(EMAIL).orElseThrow().getPhotoKey())
                .isEqualTo("accounts/second.jpg");
    }

    @Test
    void theAccountPhotoCanBeRemoved() throws Exception {
        String token = signUpAndLogin();

        mvc().perform(putPhoto(token).param("removePhoto", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").doesNotExist());

        verify(storageService).delete("accounts/probe.jpg");
        assertThat(accountRepository.findByEmailIgnoreCase(EMAIL).orElseThrow().getPhotoKey()).isNull();
    }

    /**
     * Asking to remove one while uploading another is a contradiction, and the
     * upload wins rather than leaving the account with neither.
     */
    @Test
    void anUploadWinsOverTheRemovalFlag() throws Exception {
        String token = signUpAndLogin();
        given(storageService.store(any(), anyString())).willReturn("accounts/second.jpg");
        given(storageService.urlOf("accounts/second.jpg"))
                .willReturn("https://cdn.example.test/accounts/second.jpg");

        mvc().perform(putPhoto(token)
                        .file(new MockMultipartFile("photo", "new.jpg", MediaType.IMAGE_JPEG_VALUE,
                                "another".getBytes()))
                        .param("removePhoto", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("https://cdn.example.test/accounts/second.jpg"));
    }

    /**
     * Your own account and nobody else's: there is no id in the path, so the
     * token decides whose photo this is.
     */
    @Test
    void changingAPhotoNeedsASession() throws Exception {
        mvc().perform(multipart("/api/auth/me/photo").with(r -> {
                    r.setMethod("PUT");
                    return r;
                }).param("removePhoto", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signsUpLogsInAndRegistersAnOrganization() throws Exception {
        String token = signUpAndLogin();

        mvc().perform(multipart("/api/organizations")
                        .file(organizationPart())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value(SLUG))
                .andExpect(jsonPath("$.subdomain").value(SLUG + ".jvm.my.id"));

        // The registering account owns the tenant and gained an OWNER membership.
        Account owner = accountRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(tenantRegistrationRepository.findBySlug(SLUG).orElseThrow().getOwner().getId())
                .isEqualTo(owner.getId());
        assertThat(userTenantRepository.existsByAccountIdAndTenantSlug(owner.getId(), SLUG)).isTrue();
    }

    @Test
    void freshTokenCarriesTheNewMembershipAndOpensTheTenant() throws Exception {
        String token = signUpAndLogin();
        mvc().perform(multipart("/api/organizations")
                        .file(organizationPart())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        // Logging in again picks up the membership created a moment ago.
        String body = mvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.LoginRequest(EMAIL, PASSWORD))))
                .andReturn().getResponse().getContentAsString();
        String refreshedToken = objectMapper.readTree(body).get("accessToken").asText();
        assertThat(objectMapper.readTree(body).get("memberships").get(SLUG).asText()).isEqualTo("OWNER");

        mvc().perform(get("/organization/1")
                        .header("Authorization", "Bearer " + refreshedToken)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, SLUG))
                .andExpect(status().isOk());
    }

    @Test
    void tokenWithoutTheMembershipCannotReachTheTenant() throws Exception {
        String token = signUpAndLogin();

        // orgtest1 exists but this account was never granted it.
        mvc().perform(get("/organization/1")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, "orgtest1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantRequestsWithoutATokenAreRejected() throws Exception {
        mvc().perform(get("/organization/1")
                        .header(TenantSubdomainInterceptor.TENANT_HEADER, "orgtest1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signupRejectsADuplicateEmailAndAWeakPassword() throws Exception {
        signUpAndLogin();

        MockMultipartFile duplicate = new MockMultipartFile("account", "account", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new AuthController.SignUpRequest(EMAIL, "+62 812 0000 0000", PASSWORD)));
        mvc().perform(multipart("/api/auth/signup").file(duplicate))
                .andExpect(status().isConflict());

        MockMultipartFile weak = new MockMultipartFile("account", "account", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(
                        new AuthController.SignUpRequest("other.probe@example.test", "+62 812 0000 0000", "short")));
        mvc().perform(multipart("/api/auth/signup").file(weak))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongPasswordIsRefused() throws Exception {
        signUpAndLogin();

        mvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.LoginRequest(EMAIL, "not-the-password"))))
                .andExpect(status().isUnauthorized());
    }
}
