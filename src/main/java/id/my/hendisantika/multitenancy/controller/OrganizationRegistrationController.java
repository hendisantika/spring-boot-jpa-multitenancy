package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.config.TenantSecurity;
import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.OrgStructure;
import id.my.hendisantika.multitenancy.entity.central.PracticeSpeciality;
import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.entity.central.TenantRole;
import id.my.hendisantika.multitenancy.entity.central.UserTenant;
import id.my.hendisantika.multitenancy.service.AuthService;
import id.my.hendisantika.multitenancy.service.EmailVerificationException;
import id.my.hendisantika.multitenancy.service.MembershipService;
import id.my.hendisantika.multitenancy.service.InvitationService;
import id.my.hendisantika.multitenancy.service.OrganizationProfile;
import id.my.hendisantika.multitenancy.service.OrganizationProfileService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
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
    private final InvitationService invitationService;
    private final OrganizationProfileService organizationProfileService;
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
        // Provisioning a database on an unproved address is how junk tenants get
        // created, so this is the gate verification exists for.
        if (!owner.isEmailVerified()) {
            throw new EmailVerificationException(
                    "Confirm your email address before registering an organization");
        }
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

    /**
     * Replaces the profile. Owner only, and the slug, database name and
     * subdomain stay as they are: they are the tenant's identity, so a new
     * business name changes the label and nothing else.
     * <p>
     * Omitting the photo part keeps the current photo; sending one replaces it
     * and the previous object is deleted.
     */
    @PutMapping(path = "/{slug}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OrganizationView update(@PathVariable String slug,
                                   @Valid @RequestPart("organization") RegisterOrganizationRequest request,
                                   @RequestPart(value = "photo", required = false) MultipartFile photo) {
        tenantSecurity.requireOwner(slug);
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

        return viewOf(organizationProfileService.update(slug, profile, photoKey));
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

    @GetMapping("/{slug}/invitations")
    public List<InvitationSummary> invitations(@PathVariable String slug) {
        tenantSecurity.requireOwner(slug);
        return invitationService.pendingFor(slug).stream()
                .map(invitation -> new InvitationSummary(
                        invitation.getId(),
                        invitation.getEmail(),
                        invitation.getRole(),
                        invitation.getExpiresAt()))
                .toList();
    }

    /**
     * The accept link is returned only when the email did not go out. Once the
     * recipient's mailbox has it, the owner has no reason to hold a credential
     * that would let them accept on that person's behalf.
     */
    @PostMapping("/{slug}/invitations")
    public ResponseEntity<CreatedInvitationView> invite(@PathVariable String slug,
                                                        @Valid @RequestBody InvitationController.InviteRequest request) {
        tenantSecurity.requireOwner(slug);
        Account invitedBy = authService.accountOf(tenantSecurity.currentToken().getSubject());
        InvitationService.CreatedInvitation created =
                invitationService.invite(slug, request.email(), request.role(), invitedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedInvitationView(
                created.invitation().getId(),
                created.invitation().getEmail(),
                created.invitation().getRole(),
                created.invitation().getExpiresAt(),
                created.delivered(),
                created.delivered() ? null : created.acceptUrl()));
    }

    @DeleteMapping("/{slug}/invitations/{invitationId}")
    public ResponseEntity<Void> revokeInvitation(@PathVariable String slug, @PathVariable Long invitationId) {
        tenantSecurity.requireOwner(slug);
        invitationService.revoke(slug, invitationId);
        return ResponseEntity.noContent().build();
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

    public record InvitationSummary(Long id, String email, TenantRole role, Instant expiresAt) {
    }

    /**
     * acceptUrl is present only when emailed is false, and even then it exists
     * once: the token is stored as a hash and cannot be read back.
     */
    public record CreatedInvitationView(
            Long id, String email, TenantRole role, Instant expiresAt, boolean emailed, String acceptUrl) {
    }
}
