package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.config.TenantSecurity;
import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.Invitation;
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
import id.my.hendisantika.multitenancy.service.TenantRecordNotFoundException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Organizations", description = "Registering organizations and running their membership and invitation lists. Listing is scoped to the caller's memberships.")
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
    @Operation(summary = "Organizations you belong to", description = "List the organizations the signed-in account is a member of.")
    public List<OrganizationView> mine() {
        return membershipService.organizationsOf(tenantSecurity.currentAccountId()).stream()
                .map(this::viewOf)
                .toList();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "One organization", description = "Return a single organization the caller belongs to.")
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
    @Operation(summary = "Register an organization", description = "Create an organization; the caller becomes its OWNER, and its database and subdomain are created before this returns. Multipart, so the logo arrives with it.")
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
     * and the previous object is deleted; {@code removePhoto=true} drops it.
     * <p>
     * Sending both a photo and the flag is a contradiction, and the upload wins.
     */
    @PutMapping(path = "/{slug}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Edit an organization", description = "Replace the organization profile. Owner only; the slug, database name and subdomain stay as they are.")
    public OrganizationView update(@PathVariable String slug,
                                   @Valid @RequestPart("organization") RegisterOrganizationRequest request,
                                   @RequestPart(value = "photo", required = false) MultipartFile photo,
                                   @RequestParam(value = "removePhoto", defaultValue = "false")
                                   boolean removePhoto) {
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

        return viewOf(organizationProfileService.update(slug, profile, photoKey, removePhoto));
    }

    /**
     * The photo on its own, for the card on the organization page.
     * <p>
     * The same three rules as everywhere else — omitting keeps, sending
     * replaces, {@code removePhoto=true} drops — but without the profile, which
     * otherwise had to be re-sent in full to change a picture. Re-sending it is
     * also how the other eight fields get overwritten with whatever the form
     * was holding, so this is the safer way round as well as the shorter one.
     */
    @PutMapping(path = "/{slug}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Change an organization's photo", description = "Replace or remove just the organization's photo. Owner only; multipart, removePhoto=true drops it.")
    public OrganizationView updatePhoto(@PathVariable String slug,
                                        @RequestPart(value = "photo", required = false) MultipartFile photo,
                                        @RequestParam(value = "removePhoto", defaultValue = "false")
                                        boolean removePhoto) {
        tenantSecurity.requireOwner(slug);
        String photoKey = photo != null && !photo.isEmpty() ? storageService.store(photo, PHOTO_PREFIX) : null;
        return viewOf(organizationProfileService.updatePhoto(slug, photoKey, removePhoto));
    }

    /**
     * Paged rather than whole, for the reason the tenant's own lists are: a
     * membership list only grows, and an endpoint that hands back all of it is
     * one nobody can withdraw later.
     *
     * @param q    matched against the address and the role, so "own" finds the
     *             owners without anybody typing OWNER
     * @param role narrows to these; repeat it — {@code ?role=OWNER&role=MEMBER}
     *             — and it means either of them, while still narrowing whatever
     *             {@code q} asked for
     * @param page zero based
     * @param size clamped, so a client cannot ask for the lot in one go
     */
    @GetMapping("/{slug}/users")
    @Operation(summary = "A page of members", description = "List an organization's memberships; q searches the address and role, role filters. Member only.")
    public PageResponse<MemberView> members(@PathVariable String slug,
                                            @RequestParam(name = "q", required = false) String q,
                                            @RequestParam(name = "role", required = false) List<String> role,
                                            @RequestParam(name = "page", required = false) Integer page,
                                            @RequestParam(name = "size", required = false) Integer size) {
        tenantSecurity.requireMember(slug);
        return PageResponse.of(membershipService.membersOf(slug, q, role, page, size).map(this::viewOf));
    }

    /**
     * One invitation, for the screen that shows the whole of it. Owner only,
     * like the list it comes from.
     * <p>
     * Whatever became of it, not only pending ones: a page opened while it was
     * still pending should say it has since been withdrawn or accepted rather
     * than turning into "does not exist".
     */
    @GetMapping("/{slug}/invitations/{invitationId}")
    @Operation(summary = "One invitation", description = "Return a single invitation of the organization, whatever became of it. Owner only.")
    public InvitationDetailView invitation(@PathVariable String slug, @PathVariable Long invitationId) {
        tenantSecurity.requireOwner(slug);
        Invitation invitation = invitationService.oneOf(slug, invitationId);
        return new InvitationDetailView(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getStatus().name(),
                invitation.isExpired(),
                invitation.getInvitedBy() == null ? null : invitation.getInvitedBy().getEmail(),
                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitationService.accountExistsFor(invitation));
    }

    /**
     * One membership, for the screen that shows the whole of it.
     * <p>
     * Any member may read it: these are colleagues in one organization, and the
     * list already shows everybody's address and face. Missing is 404 rather
     * than an empty body, the same as a person or a unit.
     */
    @GetMapping("/{slug}/users/{accountId}")
    @Operation(summary = "One membership", description = "Return a single membership; 404 when that account is not a member here. Member only.")
    public MemberDetailView member(@PathVariable String slug, @PathVariable Long accountId) {
        tenantSecurity.requireMember(slug);
        // Queried, not filtered out of the list: the list is a page now, and
        // walking it would have found only whoever landed on the first one.
        return membershipService.memberOf(slug, accountId)
                .filter(membership -> membership.getAccount() != null)
                .map(this::detailOf)
                .orElseThrow(() -> new TenantRecordNotFoundException(
                        "No member of '" + slug + "' with account id " + accountId));
    }

    /**
     * Only the owner may add people, which is what separates OWNER from MEMBER.
     */
    @PostMapping("/{slug}/users")
    @Operation(summary = "Add a member directly", description = "Add a person to the organization, setting their password. Owner only.")
    public ResponseEntity<MemberView> addMember(@PathVariable String slug,
                                                @Valid @RequestBody AddMemberRequest request) {
        tenantSecurity.requireOwner(slug);
        UserTenant membership = membershipService.addMember(
                slug, request.email(), request.phoneNumber(), request.password(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(viewOf(membership));
    }

    /**
     * Paged and searchable like the membership list beside it: an organization
     * that invites people steadily accumulates pending invitations, and reading
     * them one screen at a time is the same problem.
     *
     * <b>Every state, unless asked otherwise.</b> This answered the pending
     * ones and nothing else until it gained a status filter, which cannot live
     * alongside that: narrowing PENDING by ACCEPTED is always empty, and a
     * filter that can only return nothing is a filter that lies. Callers that
     * want the pending ones ask for them, which is what the screen does.
     *
     * @param q      matched against the address and the role
     * @param status narrows to these; repeat it and it means either, while
     *               still narrowing whatever {@code q} asked for
     * @param role   narrows the same way, and is AND'd with the states: owners
     *               still pending means both, not either
     * @param page   zero based
     * @param size   clamped, so a client cannot ask for the lot in one go
     */
    @GetMapping("/{slug}/invitations")
    @Operation(summary = "A page of invitations", description = "List an organization's invitations; q searches, status and role filter. Owner only.")
    public PageResponse<InvitationSummary> invitations(
            @PathVariable String slug,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) List<String> status,
            @RequestParam(name = "role", required = false) List<String> role,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        tenantSecurity.requireOwner(slug);
        return PageResponse.of(
                invitationService.invitationsOf(slug, q, status, role, page, size).map(this::summaryOf));
    }

    /**
     * The accept link is returned only when the email did not go out. Once the
     * recipient's mailbox has it, the owner has no reason to hold a credential
     * that would let them accept on that person's behalf.
     */
    @PostMapping("/{slug}/invitations")
    @Operation(summary = "Invite someone", description = "Invite a person to the organization and return the accept link. Owner only.")
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
    @Operation(summary = "Withdraw an invitation", description = "Revoke an outstanding invitation. Owner only.")
    public ResponseEntity<Void> revokeInvitation(@PathVariable String slug, @PathVariable Long invitationId) {
        tenantSecurity.requireOwner(slug);
        invitationService.revoke(slug, invitationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{slug}/users/{accountId}")
    @Operation(summary = "Remove a member", description = "Remove a person from the organization. Owner only.")
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

    /**
     * The photo comes from the account, not the membership: one account can be a
     * member of several organizations and has one photo across all of them.
     * <p>
     * A signed URL, like every other read of a stored object, so it is built
     * here rather than kept anywhere.
     */
    private MemberView viewOf(UserTenant membership) {
        Account account = membership.getAccount();
        return new MemberView(
                account == null ? null : account.getId(),
                // The account's address, not the membership's: user_name holds
                // the one it was granted to, and an email change leaves that
                // behind. The detail screen already showed the current one, so
                // the list and the detail disagreed about who somebody is.
                account == null ? membership.getUserName() : account.getEmail(),
                membership.getRole(),
                account == null ? null : storageService.urlOf(account.getPhotoKey()));
    }

    private InvitationSummary summaryOf(Invitation invitation) {
        return new InvitationSummary(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getExpiresAt(),
                invitation.getStatus().name(),
                // Not a status, and the list can no longer be assumed pending:
                // a row has to say for itself whether it still works.
                invitation.isExpired(),
                invitationService.accountExistsFor(invitation));
    }

    private MemberDetailView detailOf(UserTenant membership) {
        Account account = membership.getAccount();
        return new MemberDetailView(
                account.getId(),
                // The membership carries the address it was granted to; the
                // account is where it may since have moved to, and that is the
                // one worth showing.
                account.getEmail(),
                membership.getRole(),
                storageService.urlOf(account.getPhotoKey()),
                account.getPhoneNumber(),
                account.isEmailVerified(),
                membership.getCreatedAt());
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

    public record MemberView(Long accountId, String email, TenantRole role, String photoUrl) {
    }

    /**
     * @param joinedAt null on memberships that predate the column being added,
     *                 which is not the same as having joined at no time and is
     *                 shown as unknown rather than guessed at
     */
    public record MemberDetailView(
            Long accountId, String email, TenantRole role, String photoUrl,
            String phoneNumber, boolean emailVerified, Instant joinedAt) {
    }

    /**
     * No photo, deliberately. An invited address may belong to somebody who is
     * not a member of anything here and has agreed to nothing, so what the
     * owner gets is whether it is registered and not who it is.
     *
     * @param accountExists whether accepting grants an existing account or
     *                      makes one
     */
    public record InvitationSummary(Long id, String email, TenantRole role, Instant expiresAt,
                                    String status, boolean expired, boolean accountExists) {
    }

    /**
     * No accept link: the token is stored as a hash and cannot be read back, so
     * it exists in the recipient's mailbox and nowhere else.
     *
     * @param expired       PENDING but past its date, which the status alone
     *                      does not say — nothing sweeps them
     * @param accountExists whether accepting grants an account that is already
     *                      registered or makes a new one
     */
    public record InvitationDetailView(
            Long id, String email, TenantRole role, String status, boolean expired,
            String invitedBy, Instant createdAt, Instant expiresAt, Instant acceptedAt,
            boolean accountExists) {
    }

    /**
     * acceptUrl is present only when emailed is false, and even then it exists
     * once: the token is stored as a hash and cannot be read back.
     */
    public record CreatedInvitationView(
            Long id, String email, TenantRole role, Instant expiresAt, boolean emailed, String acceptUrl) {
    }
}
