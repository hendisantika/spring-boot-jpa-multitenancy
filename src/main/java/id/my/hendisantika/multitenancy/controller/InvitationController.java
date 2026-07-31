package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.TenantRole;
import id.my.hendisantika.multitenancy.entity.central.UserTenant;
import id.my.hendisantika.multitenancy.service.AuthService;
import id.my.hendisantika.multitenancy.service.InvitationService;
import id.my.hendisantika.multitenancy.service.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The recipient's half of the invitation flow, which is open: the token is the
 * only credential, and whoever holds it has not signed in yet.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 09.14
 */
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;
    private final AuthService authService;
    private final TokenService tokenService;

    /**
     * What the accept page shows before anyone commits to anything.
     */
    @GetMapping("/{token}")
    public InvitationView preview(@PathVariable String token) {
        InvitationService.InvitationPreview preview = invitationService.preview(token);
        return new InvitationView(
                preview.email(),
                preview.role(),
                preview.tenantSlug(),
                preview.organizationName(),
                preview.accountExists(),
                preview.expiresAt());
    }

    /**
     * Accepting signs the recipient in, so they land inside the organization
     * rather than at a login form.
     */
    @PostMapping("/{token}/accept")
    public AuthController.TokenPair accept(@PathVariable String token,
                                           @Valid @RequestBody AcceptRequest request) {
        Account account = invitationService.accept(token, request.password());
        List<UserTenant> memberships = authService.membershipsOf(account);
        Map<String, String> byTenant = memberships.stream()
                .collect(Collectors.toMap(UserTenant::getTenantSlug, m -> m.getRole().name(), (a, b) -> a));
        return new AuthController.TokenPair(
                tokenService.issueAccessToken(account, memberships),
                tokenService.issueRefreshToken(account),
                byTenant);
    }

    /**
     * @param password only needed when the recipient has no account yet; an
     *                 existing account keeps the password it already has
     */
    public record AcceptRequest(@Size(min = 8, max = 100) String password) {
    }

    public record InvitationView(
            String email,
            TenantRole role,
            String tenantSlug,
            String organizationName,
            boolean accountExists,
            Instant expiresAt) {
    }

    /**
     * Kept separate so the owner side can validate its own payload.
     */
    public record InviteRequest(
            @NotBlank @jakarta.validation.constraints.Email @Size(max = 255) String email,
            TenantRole role
    ) {
    }
}
