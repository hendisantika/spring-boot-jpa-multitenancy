package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
import id.my.hendisantika.multitenancy.service.AuthService;
import id.my.hendisantika.multitenancy.service.TenantProvisioningService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Provisioning surface for tenants. Phase 2 moves this behind the owner's
 * authenticated session; for now it is the seam that creates a database and a
 * subdomain for an organization.
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
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantProvisioningService tenantProvisioningService;
    private final AuthService authService;
    private final TenantRegistrationRepository tenantRegistrationRepository;

    @GetMapping
    public List<TenantView> list() {
        return tenantRegistrationRepository.findAll().stream().map(TenantView::of).toList();
    }

    /**
     * The authenticated account becomes the owner of the organization it
     * registers, and gains an OWNER membership for it.
     */
    @PostMapping
    public ResponseEntity<TenantView> create(@Valid @RequestBody CreateTenantRequest request,
                                             @AuthenticationPrincipal Jwt jwt) {
        Account owner = authService.accountOf(jwt.getSubject());
        TenantRegistration tenant = tenantProvisioningService.provision(request.name(), owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantView.of(tenant));
    }

    public record CreateTenantRequest(
            @NotBlank @Size(max = 100) String name
    ) {
    }

    public record TenantView(String slug, String databaseName, String subdomain, String displayName, String status) {

        static TenantView of(TenantRegistration tenant) {
            return new TenantView(
                    tenant.getSlug(),
                    tenant.getDatabaseName(),
                    tenant.getSubdomain(),
                    tenant.getDisplayName(),
                    tenant.getStatus().name()
            );
        }
    }
}
