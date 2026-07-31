package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.UserTenant;
import id.my.hendisantika.multitenancy.service.AuthService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final StorageService storageService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    /**
     * Multipart so the profile photo arrives with the rest of the details, as a
     * JSON part named "account" plus an optional file part named "photo".
     */
    @PostMapping(path = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AccountView> signUp(@Valid @RequestPart("account") SignUpRequest request,
                                              @RequestPart(value = "photo", required = false) MultipartFile photo) {
        Account account = authService.signUp(request.email(), request.phoneNumber(), request.password(), photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(viewOf(account));
    }

    @PostMapping("/login")
    public TokenPair login(@Valid @RequestBody LoginRequest request) {
        return tokensFor(authService.authenticate(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    public TokenPair refresh(@Valid @RequestBody RefreshRequest request) {
        return tokensFor(authService.accountFromRefreshToken(request.refreshToken()));
    }

    /**
     * Answers the same way whether or not the address has an account, so it
     * cannot be used to find out who is registered. resetUrl comes back only
     * when mail delivery is off, so the flow is still followable locally.
     */
    @PostMapping("/password/forgot")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return new ForgotPasswordResponse(
                "If that address has an account, a reset link is on its way.",
                passwordResetService.request(request.email()).orElse(null));
    }

    /**
     * Shows whose account a link belongs to, before anything is changed.
     */
    @GetMapping("/password/reset/{token}")
    public ResetPasswordView previewReset(@PathVariable String token) {
        return new ResetPasswordView(passwordResetService.emailFor(token));
    }

    @PostMapping("/password/reset/{token}")
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
    public VerifiedView verifyEmail(@PathVariable String token) {
        return new VerifiedView(emailVerificationService.verify(token));
    }

    /**
     * Sends a fresh link, invalidating the earlier one.
     */
    @PostMapping("/verify-email/resend")
    public ForgotPasswordResponse resendVerification(@AuthenticationPrincipal Jwt jwt) {
        Account account = authService.accountOf(jwt.getSubject());
        return new ForgotPasswordResponse(
                "Verification link sent.",
                emailVerificationService.resendFor(account).orElse(null));
    }

    @GetMapping("/me")
    public AccountView me(@AuthenticationPrincipal Jwt jwt) {
        return viewOf(authService.accountOf(jwt.getSubject()));
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
                account.isEmailVerified());
    }

    public record SignUpRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{6,30}$", message = "must be a phone number")
            String phoneNumber,
            @NotBlank @Size(min = 8, max = 100, message = "must be at least 8 characters") String password
    ) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record TokenPair(String accessToken, String refreshToken, Map<String, String> memberships) {
    }

    public record AccountView(Long id, String email, String phoneNumber, String photoUrl, String status,
                              boolean emailVerified) {
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
