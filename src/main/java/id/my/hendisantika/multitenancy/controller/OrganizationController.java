package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.Organization;
import id.my.hendisantika.multitenancy.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
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

    @GetMapping(value = "/organization/{id}")
    public Organization getOrganization(@PathVariable("id") Long id) {
        Optional<Organization> organization = organizationService.findById(id);
        return organization.orElse(new Organization());
    }
}
