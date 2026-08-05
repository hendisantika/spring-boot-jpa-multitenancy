package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.EmailChange;
import id.my.hendisantika.multitenancy.entity.central.UserTenant;
import id.my.hendisantika.multitenancy.service.AuthService;
import id.my.hendisantika.multitenancy.service.EmailChangeService;
import id.my.hendisantika.multitenancy.service.EmailVerificationService;
import id.my.hendisantika.multitenancy.service.PasswordResetService;
import id.my.hendisantika.multitenancy.service.TokenService;
import id.my.hendisantika.multitenancy.service.storage.StorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Signup and the parent login. Every user of every tenant authenticates here.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Signing up, signing in, token refresh, and self-service account management — email, phone and password.")
public class AuthController {

    /**
     * One rule for an account's phone number, so signing up and correcting it
     * later cannot drift apart.
     */
    private static final String PHONE_PATTERN = "^\\+?[0-9 ()-]{6,30}$";

    private static final String PHONE_MESSAGE =
            "must be digits, spaces, brackets or dashes, optionally starting with +";

    private final AuthService authService;
    private final TokenService tokenService;
    private final StorageService storageService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final EmailChangeService emailChangeService;

    /**
     * Multipart so the profile photo arrives with the rest of the details, as a
     * JSON part named "account" plus an optional file part named "photo".
     */
    @PostMapping(path = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Register an owner account", description = "Create an account from email, phone, password and an optional photo; the caller can then register an organization.")
    public ResponseEntity<AccountView> signUp(@Valid @RequestPart("account") SignUpRequest request,
                                              @RequestPart(value = "photo", required = false) MultipartFile photo) {
        Account account = authService.signUp(request.email(), request.phoneNumber(), request.password(), photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(viewOf(account));
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in", description = "Exchange email and password for an access and refresh token pair.")
    public TokenPair login(@Valid @RequestBody LoginRequest request) {
        return tokensFor(authService.authenticate(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Exchange a valid refresh token for a fresh access and refresh token pair.")
    public TokenPair refresh(@Valid @RequestBody RefreshRequest request) {
        return tokensFor(authService.accountFromRefreshToken(request.refreshToken()));
    }

    /**
     * Answers the same way whether or not the address has an account, so it
     * cannot be used to find out who is registered. resetUrl comes back only
     * when mail delivery is off, so the flow is still followable locally.
     */
    @PostMapping("/password/forgot")
    @Operation(summary = "Request a password reset", description = "Send a reset link to the address if it has an account; the response is the same whether it does or not, so it reveals nothing.")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return new ForgotPasswordResponse(
                "If that address has an account, a reset link is on its way.",
                passwordResetService.request(request.email()).orElse(null));
    }

    /**
     * Shows whose account a link belongs to, before anything is changed.
     */
    @GetMapping("/password/reset/{token}")
    @Operation(summary = "Preview a password reset", description = "Return the address a reset token belongs to, for the reset screen, before a new password is set.")
    public ResetPasswordView previewReset(@PathVariable String token) {
        return new ResetPasswordView(passwordResetService.emailFor(token));
    }

    @PostMapping("/password/reset/{token}")
    @Operation(summary = "Set a new password from a reset link", description = "Consume a reset token and set the account's new password.")
    public ResetPasswordView resetPassword(@PathVariable String token,
                                           @Valid @RequestBody ResetPasswordRequest request) {
        String email = passwordResetService.emailFor(token);
        passwordResetService.reset(token, request.password());
        return new ResetPasswordView(email);
    }

    /**
     * Open: the token in the link is the only credential, and whoever opens it
     * may not be signed in.
     */
    @PostMapping("/verify-email/{token}")
    @Operation(summary = "Confirm an email address", description = "Consume an email-verification token and mark the address confirmed.")
    public VerifiedView verifyEmail(@PathVariable String token) {
        return new VerifiedView(emailVerificationService.verify(token));
    }

    /**
     * Sends a fresh link, invalidating the earlier one.
     */
    @PostMapping("/verify-email/resend")
    @Operation(summary = "Resend the verification link", description = "Send a fresh email-verification link to the signed-in account.")
    public ForgotPasswordResponse resendVerification(@AuthenticationPrincipal Jwt jwt) {
        Account account = authService.accountOf(jwt.getSubject());
        return new ForgotPasswordResponse(
                "Verification link sent.",
                emailVerificationService.resendFor(account).orElse(null));
    }

    @GetMapping("/me")
    @Operation(summary = "The signed-in account", description = "Return the account the access token was issued for.")
    public AccountView me(@AuthenticationPrincipal Jwt jwt) {
        return viewOf(authService.accountOf(jwt.getSubject()));
    }

    /**
     * The photo on your own account, which signup could set and nothing could
     * change afterwards.
     * <p>
     * Omitting the part keeps the current photo, sending one replaces it, and
     * {@code removePhoto=true} drops it — the same three rules as a person and
     * an organization, so there is one thing to learn rather than three.
     */
    @PutMapping(path = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Change your photo", description = "Replace or remove the signed-in account's photo; multipart, removePhoto=true drops it.")
    public AccountView updateMyPhoto(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "removePhoto", defaultValue = "false") boolean removePhoto) {
        Account account = authService.accountOf(jwt.getSubject());
        return viewOf(authService.updatePhoto(account, photo, removePhoto));
    }

    /**
     * The phone number, which signup asked for and nothing could correct.
     * <p>
     * No confirmation and no password, unlike the address: nothing signs in with
     * this and nothing is sent to it, so there is nothing to prove first.
     */
    @PutMapping("/me/phone")
    @Operation(summary = "Change your phone number", description = "Update the signed-in account's phone number.")
    public AccountView updateMyPhoneNumber(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody PhoneNumberRequest request) {
        Account account = authService.accountOf(jwt.getSubject());
        return viewOf(authService.updatePhoneNumber(account, request.phoneNumber()));
    }

    /**
     * Changes the password from inside a session, which the reset link could
     * only do from outside one.
     * <p>
     * Answers with a fresh pair rather than nothing: stamping the change disowns
     * every refresh token issued before it, which is the point, but that would
     * otherwise include the session doing the changing.
     */
    @PutMapping("/me/password")
    @Operation(summary = "Change your password", description = "Change the password from inside a session and receive a fresh token pair; the current password is required.")
    public TokenPair changeMyPassword(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody PasswordChangeRequest request) {
        Account account = authService.accountOf(jwt.getSubject());
        return tokensFor(authService.changePassword(
                account, request.currentPassword(), request.newPassword()));
    }

    /**
     * Asks to move the account to a different address.
     * <p>
     * Nothing changes here: the address is only recorded, and the account keeps
     * signing in as it does until the link sent to the new mailbox is opened.
     * The current password is required, because this is the credential itself
     * and a stolen session alone should not be enough to take an account over.
     * <p>
     * confirmUrl comes back only when mail delivery is off, so the flow is still
     * followable locally.
     */
    @PostMapping("/me/email")
    @Operation(summary = "Request an email change", description = "Start moving the account to a new address; a confirmation link is sent to the new mailbox. The current password is required.")
    public EmailChangeResponse requestEmailChange(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody EmailChangeRequest request) {
        Account account = authService.accountOf(jwt.getSubject());
        String link = emailChangeService.request(account, request.email(), request.password()).orElse(null);
        return new EmailChangeResponse(
                "Confirm the change from the link sent to " + request.email() + ".", link);
    }

    /**
     * Drops the outstanding request, for whoever typed the address wrong and
     * would rather not wait a day for the link to lapse.
     */
    @DeleteMapping("/me/email")
    @Operation(summary = "Cancel an email change", description = "Drop an outstanding email-change request.")
    public ResponseEntity<Void> cancelEmailChange(@AuthenticationPrincipal Jwt jwt) {
        emailChangeService.cancel(authService.accountOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Open, like verification: the token in the link is the only credential, and
     * whoever opens it is reading the new mailbox, which may be a browser with no
     * session at all.
     */
    @PostMapping("/email-change/{token}")
    @Operation(summary = "Confirm a new email address", description = "Consume the token sent to the new mailbox, which is when the address actually changes.")
    public VerifiedView confirmEmailChange(@PathVariable String token) {
        return new VerifiedView(emailChangeService.confirm(token));
    }

    private TokenPair tokensFor(Account account) {
        List<UserTenant> memberships = authService.membershipsOf(account);
        Map<String, String> byTenant = memberships.stream()
                .collect(Collectors.toMap(UserTenant::getTenantSlug, m -> m.getRole().name(), (a, b) -> a));
        return new TokenPair(
                tokenService.issueAccessToken(account, memberships),
                tokenService.issueRefreshToken(account),
                byTenant);
    }

    private AccountView viewOf(Account account) {
        return new AccountView(
                account.getId(),
                account.getEmail(),
                account.getPhoneNumber(),
                storageService.urlOf(account.getPhotoKey()),
                account.getStatus().name(),
                account.isEmailVerified(),
                // So a screen can say a change is waiting rather than showing the
                // old address and looking as though nothing happened.
                emailChangeService.pendingFor(account).map(EmailChange::getNewEmail).orElse(null));
    }

    public record SignUpRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Pattern(regexp = PHONE_PATTERN, message = PHONE_MESSAGE) String phoneNumber,
            @NotBlank @Size(min = 8, max = 100, message = "must be at least 8 characters") String password
    ) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record TokenPair(String accessToken, String refreshToken, Map<String, String> memberships) {
    }

    /**
     * @param pendingEmail an address asked for and not yet confirmed, null when
     *                     there is none
     */
    public record AccountView(Long id, String email, String phoneNumber, String photoUrl, String status,
                              boolean emailVerified, String pendingEmail) {
    }

    public record PasswordChangeRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 100, message = "must be at least 8 characters") String newPassword
    ) {
    }

    public record PhoneNumberRequest(
            @NotBlank @Pattern(regexp = PHONE_PATTERN, message = PHONE_MESSAGE) String phoneNumber
    ) {
    }

    public record EmailChangeRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank String password
    ) {
    }

    /**
     * confirmUrl is null unless mail delivery is off.
     */
    public record EmailChangeResponse(String message, String confirmUrl) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 255) String email) {
    }

    /**
     * resetUrl is null unless mail delivery is off.
     */
    public record ForgotPasswordResponse(String message, String resetUrl) {
    }

    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 100) String password) {
    }

    public record ResetPasswordView(String email) {
    }

    public record VerifiedView(String email) {
    }
}
