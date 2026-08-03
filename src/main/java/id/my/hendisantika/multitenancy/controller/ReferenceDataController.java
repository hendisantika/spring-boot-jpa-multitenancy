package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.tenant.ReferenceData;
import id.my.hendisantika.multitenancy.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import id.my.hendisantika.multitenancy.service.TenantRecordNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The lists every tenant starts with, read from that tenant's own database.
 * <p>
 * Any member may read them: they are what the forms are filled in from, so
 * withholding them would leave the role unable to do the work. Nothing here
 * writes, because these arrive by migration.
 * <p>
 * Not paged, unlike the people and the units. These lists are bounded by what a
 * migration puts in them, and a client that has to page a dropdown has been
 * given the wrong shape.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 18.44
 */
@RestController
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    @GetMapping("/reference-data")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public Map<String, List<ReferenceData>> listAll() {
        return referenceDataService.findAllByCategory();
    }

    /**
     * One list, a page at a time, searchable by label or code.
     * <p>
     * <b>404 when this tenant keeps no such list</b>, which the whole map at
     * {@code /reference-data} still cannot say — an absent key there and an
     * empty list look the same. It is not a contradiction of the old behaviour
     * so much as the end of it: that answered an empty list so a dropdown would
     * not break, and no dropdown reads this endpoint. They all read the map,
     * which is unchanged.
     * <p>
     * Being told "no such list" apart from "nothing matched what you typed" is
     * the whole reason a screen can say something useful, and a page cannot
     * carry that difference on its own.
     *
     * @param q    matched against the label and the code
     * @param page zero based
     * @param size clamped, so a client cannot ask for the lot in one go
     */
    @GetMapping("/reference-data/{category}")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public PageResponse<ReferenceData> listCategory(
            @PathVariable("category") String category,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        if (!referenceDataService.hasCategory(category)) {
            throw new TenantRecordNotFoundException("This tenant keeps no list called " + category);
        }
        return PageResponse.of(referenceDataService.findPage(category, q, page, size));
    }
}
