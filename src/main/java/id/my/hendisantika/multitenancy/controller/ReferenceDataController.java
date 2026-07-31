package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.tenant.ReferenceData;
import id.my.hendisantika.multitenancy.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/reference-data/{category}")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public List<ReferenceData> listCategory(@PathVariable("category") String category) {
        return referenceDataService.findByCategory(category);
    }
}
