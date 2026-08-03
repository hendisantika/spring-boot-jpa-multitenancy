package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.config.UnknownTenantException;
import id.my.hendisantika.multitenancy.service.AccountAlreadyExistsException;
import id.my.hendisantika.multitenancy.service.AuthenticationFailedException;
import id.my.hendisantika.multitenancy.service.EmailChangeException;
import id.my.hendisantika.multitenancy.service.EmailVerificationException;
import id.my.hendisantika.multitenancy.service.InvitationException;
import id.my.hendisantika.multitenancy.service.PasswordChangeException;
import id.my.hendisantika.multitenancy.service.PasswordResetException;
import id.my.hendisantika.multitenancy.service.TenantProvisioningException;
import id.my.hendisantika.multitenancy.service.TenantRecordInvalidException;
import id.my.hendisantika.multitenancy.service.TenantRecordNotFoundException;
import id.my.hendisantika.multitenancy.service.storage.StorageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Turns the domain failures into status codes a client can act on, instead of a
 * blanket 500.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * A request that failed its own validation.
     * <p>
     * Without this the answer was the container's default body, which carries no
     * {@code detail} at all, so a client had nothing to show and a rejected form
     * looked like a form that had quietly done nothing. Naming the fields is
     * safe here: these are constraints the client already knows about, since it
     * had to satisfy them to succeed.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onInvalidRequest(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> readable(error.getField()) + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                detail.isEmpty() ? "That request is not valid" : detail);
    }

    /**
     * {@code phoneNumber} to "Phone number". The field name goes in front of the
     * message, and this is read by whoever typed the value, not by the developer
     * who named the field.
     */
    private static String readable(String field) {
        String spaced = field.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ").toLowerCase(Locale.ROOT);
        return spaced.isEmpty() ? spaced : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    /**
     * Deliberately vague: it never says whether it was the email or the password.
     */
    @ExceptionHandler(AuthenticationFailedException.class)
    public ProblemDetail onAuthenticationFailed(AuthenticationFailedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ProblemDetail onAccountExists(AccountAlreadyExistsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * One message for every invalid token, so a caller holding a bad one learns
     * nothing about whether it ever existed.
     */
    @ExceptionHandler(EmailVerificationException.class)
    public ProblemDetail onVerificationFailed(EmailVerificationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(EmailChangeException.class)
    public ProblemDetail onEmailChangeFailed(EmailChangeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(PasswordChangeException.class)
    public ProblemDetail onPasswordChangeFailed(PasswordChangeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(PasswordResetException.class)
    public ProblemDetail onPasswordResetFailed(PasswordResetException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvitationException.class)
    public ProblemDetail onInvitationFailed(InvitationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(TenantProvisioningException.class)
    public ProblemDetail onProvisioningFailed(TenantProvisioningException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(StorageException.class)
    public ProblemDetail onStorageFailed(StorageException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * A role or membership check failed. The message names what was required but
     * never whether the organization exists.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail onAccessDenied(AccessDeniedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(TenantRecordNotFoundException.class)
    public ProblemDetail onTenantRecordMissing(TenantRecordNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(TenantRecordInvalidException.class)
    public ProblemDetail onTenantRecordInvalid(TenantRecordInvalidException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(UnknownTenantException.class)
    public ProblemDetail onUnknownTenant(UnknownTenantException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
