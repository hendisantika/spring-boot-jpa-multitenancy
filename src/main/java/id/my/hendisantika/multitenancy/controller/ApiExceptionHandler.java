package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.config.UnknownTenantException;
import id.my.hendisantika.multitenancy.service.AccountAlreadyExistsException;
import id.my.hendisantika.multitenancy.service.AuthenticationFailedException;
import id.my.hendisantika.multitenancy.service.EmailVerificationException;
import id.my.hendisantika.multitenancy.service.InvitationException;
import id.my.hendisantika.multitenancy.service.PasswordResetException;
import id.my.hendisantika.multitenancy.service.TenantProvisioningException;
import id.my.hendisantika.multitenancy.service.storage.StorageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(UnknownTenantException.class)
    public ProblemDetail onUnknownTenant(UnknownTenantException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
