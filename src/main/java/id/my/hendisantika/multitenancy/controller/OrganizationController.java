package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.tenant.Organization;
import id.my.hendisantika.multitenancy.service.OrganizationService;
import id.my.hendisantika.multitenancy.service.TenantRecordNotFoundException;
import id.my.hendisantika.multitenancy.service.UnitFilter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import id.my.hendisantika.multitenancy.service.storage.StorageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Units", description = "The tenant's organizational units, held in its own database. Every call is scoped to the resolved tenant.")
public class OrganizationController {

    private static final String PHOTO_PREFIX = "units";

    private final OrganizationService organizationService;

    private final StorageService storageService;

    @GetMapping("/organization/{id}")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    @Operation(summary = "One unit", description = "Return a unit by id from the tenant's database; 404 when there is none.")
    public UnitView getOrganization(@PathVariable("id") Long id) {
        return organizationService.findById(id).map(this::viewOf)
                .orElseThrow(() -> new TenantRecordNotFoundException("No organization with id " + id));
    }

    /**
     * Paged and searchable on the same terms as {@code /person}, so a client
     * that can read one list can read the other.
     *
     * A search widens and a filter narrows, so they combine: {@code ?q=cabang}
     * with {@code &province=BALI} means both, not either.
     * <p>
     * Every filter may be repeated — {@code ?province=BALI&province=JAWA_BARAT} —
     * and then means either of them, while still narrowing whatever else was
     * asked for.
     *
     * @param q    matched against the name, address, email and the labels behind the codes
     * @param page zero based
     * @param size clamped, so a client cannot ask for the lot in one go
     */
    @GetMapping("/organization")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    @Operation(summary = "A page of units", description = "List the tenant's units, paged and filtered.")
    public PageResponse<UnitView> listOrganizations(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "unitType", required = false) List<String> unitType,
            @RequestParam(name = "operatingStatus", required = false) List<String> operatingStatus,
            @RequestParam(name = "province", required = false) List<String> province) {
        UnitFilter filter = UnitFilter.of(unitType, operatingStatus, province);
        return PageResponse.of(organizationService.findPage(q, filter, page, size).map(this::viewOf));
    }

    @PostMapping("/organization")
    @PreAuthorize("@tenantSecurity.isOwnerOfCurrentTenant()")
    @Operation(summary = "Create a unit", description = "Create a unit from JSON. Owner only.")
    public ResponseEntity<UnitView> createOrganization(@Valid @RequestBody Organization organization) {
        return ResponseEntity.status(HttpStatus.CREATED).body(viewOf(organizationService.save(organization)));
    }

    /**
     * The same thing with a photo attached.
     * <p>
     * A second mapping rather than turning the JSON one into multipart, exactly
     * as on a person: that would break every caller already posting JSON for a
     * unit with no photo, which is most of them.
     */
    @PostMapping(path = "/organization", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@tenantSecurity.isOwnerOfCurrentTenant()")
    @Operation(summary = "Create a unit with a photo", description = "Create a unit and attach a photo; multipart. Owner only.")
    public ResponseEntity<UnitView> createOrganizationWithPhoto(
            @Valid @RequestPart("organization") Organization organization,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(viewOf(organizationService.save(organization, keyOf(photo))));
    }

    @PutMapping("/organization/{id}")
    @PreAuthorize("@tenantSecurity.isOwnerOfCurrentTenant()")
    @Operation(summary = "Update a unit", description = "Replace a unit from JSON. Owner only.")
    public UnitView updateOrganization(@PathVariable("id") Long id,
                                       @Valid @RequestBody Organization organization) {
        return viewOf(organizationService.update(id, organization));
    }

    /**
     * Omitting the photo part keeps the current one; sending one replaces it and
     * the old object is removed; {@code removePhoto=true} drops it entirely.
     * <p>
     * Sending both a photo and the flag is a contradiction, and the upload wins
     * — the same three rules as a person, an account and an organization, so
     * there is one thing to learn rather than four.
     */
    @PutMapping(path = "/organization/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@tenantSecurity.isOwnerOfCurrentTenant()")
    @Operation(summary = "Update a unit with a photo", description = "Replace a unit and its photo; multipart. Owner only.")
    public UnitView updateOrganizationWithPhoto(
            @PathVariable("id") Long id,
            @Valid @RequestPart("organization") Organization organization,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "removePhoto", defaultValue = "false") boolean removePhoto) {
        return viewOf(organizationService.update(id, organization, keyOf(photo), removePhoto));
    }

    private String keyOf(MultipartFile photo) {
        return photo != null && !photo.isEmpty() ? storageService.store(photo, PHOTO_PREFIX) : null;
    }

    private UnitView viewOf(Organization organization) {
        return UnitView.of(organization, storageService);
    }

    @DeleteMapping("/organization/{id}")
    @PreAuthorize("@tenantSecurity.isOwnerOfCurrentTenant()")
    @Operation(summary = "Delete a unit", description = "Delete a unit by id, removing its photo with it. Owner only.")
    public ResponseEntity<Void> deleteOrganization(@PathVariable("id") Long id) {
        organizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
