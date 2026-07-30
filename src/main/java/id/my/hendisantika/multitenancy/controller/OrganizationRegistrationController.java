package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.config.TenantSecurity;
import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.OrgStructure;
import id.my.hendisantika.multitenancy.entity.central.PracticeSpeciality;
import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.entity.central.TenantRole;
import id.my.hendisantika.multitenancy.entity.central.UserTenant;
import id.my.hendisantika.multitenancy.service.AuthService;
import id.my.hendisantika.multitenancy.service.MembershipService;
import id.my.hendisantika.multitenancy.service.OrganizationProfile;
import id.my.hendisantika.multitenancy.service.TenantProvisioningService;
import id.my.hendisantika.multitenancy.service.storage.StorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Registering an organization and running its membership list.
 * <p>
 * Listing is scoped to the caller's memberships: an account has no business
 * seeing organizations it does not belong to.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.28
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationRegistrationController {

    private static final String PHOTO_PREFIX = "organizations";

    private final TenantProvisioningService tenantProvisioningService;
    private final MembershipService membershipService;
    private final AuthService authService;
    private final StorageService storageService;
    private final TenantSecurity tenantSecurity;

    @GetMapping
    public List<OrganizationView> mine() {
        return membershipService.organizationsOf(tenantSecurity.currentAccountId()).stream()
                .map(this::viewOf)
                .toList();
    }

    @GetMapping("/{slug}")
    public OrganizationView one(@PathVariable String slug) {
        tenantSecurity.requireMember(slug);
        return membershipService.organizationsOf(tenantSecurity.currentAccountId()).stream()
                .filter(tenant -> tenant.getSlug().equals(slug))
                .findFirst()
                .map(this::viewOf)
                .orElseThrow(() -> new IllegalStateException("Membership without an organization: " + slug));
    }

    /**
     * The registration form, multipart so the organization photo arrives with it.
     * The caller becomes the OWNER, and the database and subdomain are created
     * before this returns.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrganizationView> register(
            @Valid @RequestPart("organization") RegisterOrganizationRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        Account owner = authService.accountOf(tenantSecurity.currentToken().getSubject());
        String photoKey = photo != null && !photo.isEmpty() ? storageService.store(photo, PHOTO_PREFIX) : null;

        OrganizationProfile profile = new OrganizationProfile(
                request.businessName(),
                request.businessEmail(),
                photoKey,
                request.contactFirstName(),
                request.contactLastName(),
                request.jobTitle(),
                request.phoneNumber(),
                request.orgStructure(),
                request.practiceSpeciality());

        TenantRegistration tenant = tenantProvisioningService.provision(profile, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(viewOf(tenant));
    }

    @GetMapping("/{slug}/users")
    public List<MemberView> members(@PathVariable String slug) {
        tenantSecurity.requireMember(slug);
        return membershipService.membersOf(slug).stream().map(this::viewOf).toList();
    }

    /**
     * Only the owner may add people, which is what separates OWNER from MEMBER.
     */
    @PostMapping("/{slug}/users")
    public ResponseEntity<MemberView> addMember(@PathVariable String slug,
                                                @Valid @RequestBody AddMemberRequest request) {
        tenantSecurity.requireOwner(slug);
        UserTenant membership = membershipService.addMember(
                slug, request.email(), request.phoneNumber(), request.password(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(viewOf(membership));
    }

    @DeleteMapping("/{slug}/users/{accountId}")
    public ResponseEntity<Void> removeMember(@PathVariable String slug, @PathVariable Long accountId) {
        tenantSecurity.requireOwner(slug);
        membershipService.removeMember(slug, accountId);
        return ResponseEntity.noContent().build();
    }

    private OrganizationView viewOf(TenantRegistration tenant) {
        return new OrganizationView(
                tenant.getSlug(),
                tenant.getDisplayName(),
                tenant.getBusinessEmail(),
                storageService.urlOf(tenant.getPhotoKey()),
                tenant.getContactFirstName(),
                tenant.getContactLastName(),
                tenant.getJobTitle(),
                tenant.getPhoneNumber(),
                tenant.getOrgStructure(),
                tenant.getPracticeSpeciality(),
                tenant.getDatabaseName(),
                tenant.getSubdomain(),
                tenant.getStatus().name());
    }

    private MemberView viewOf(UserTenant membership) {
        return new MemberView(
                membership.getAccount() == null ? null : membership.getAccount().getId(),
                membership.getUserName(),
                membership.getRole());
    }

    public record RegisterOrganizationRequest(
            @NotBlank @Size(max = 100) String businessName,
            @NotBlank @Email @Size(max = 255) String businessEmail,
            @NotBlank @Size(max = 100) String contactFirstName,
            @NotBlank @Size(max = 100) String contactLastName,
            @NotBlank @Size(max = 100) String jobTitle,
            @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{6,30}$", message = "must be a phone number")
            String phoneNumber,
            @NotNull OrgStructure orgStructure,
            @NotNull PracticeSpeciality practiceSpeciality
    ) {
    }

    public record AddMemberRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @Pattern(regexp = "^\\+?[0-9 ()-]{6,30}$", message = "must be a phone number") String phoneNumber,
            @NotBlank @Size(min = 8, max = 100, message = "must be at least 8 characters") String password,
            TenantRole role
    ) {
    }

    public record OrganizationView(
            String slug, String businessName, String businessEmail, String photoUrl,
            String contactFirstName, String contactLastName, String jobTitle, String phoneNumber,
            OrgStructure orgStructure, PracticeSpeciality practiceSpeciality,
            String databaseName, String subdomain, String status) {
    }

    public record MemberView(Long accountId, String email, TenantRole role) {
    }
}
