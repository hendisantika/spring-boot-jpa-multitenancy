package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.tenant.Organization;
import id.my.hendisantika.multitenancy.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The business units inside whichever tenant the request resolved to. Not to be
 * confused with the organization that owns the tenant, which lives centrally.
 * <p>
 * Any member may read these, but only an OWNER may change them: they are closer
 * to the shape of the business than to its daily work, so a shift should not be
 * able to rearrange them.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:56
 * To change this template use File | Settings | File Templates.
 */
@RestController
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping("/organization/{id}")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public Organization getOrganization(@PathVariable("id") Long id) {
        return organizationService.findById(id).orElse(new Organization());
    }

    @GetMapping("/organization")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public List<Organization> listOrganizations() {
        return organizationService.findAll();
    }

    @PostMapping("/organization")
    @PreAuthorize("@tenantSecurity.isOwnerOfCurrentTenant()")
    public ResponseEntity<Organization> createOrganization(@Valid @RequestBody Organization organization) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.save(organization));
    }

    @PutMapping("/organization/{id}")
    @PreAuthorize("@tenantSecurity.isOwnerOfCurrentTenant()")
    public Organization updateOrganization(@PathVariable("id") Long id,
                                           @Valid @RequestBody Organization organization) {
        return organizationService.update(id, organization);
    }

    @DeleteMapping("/organization/{id}")
    @PreAuthorize("@tenantSecurity.isOwnerOfCurrentTenant()")
    public ResponseEntity<Void> deleteOrganization(@PathVariable("id") Long id) {
        organizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
