package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.tenant.Organization;
import id.my.hendisantika.multitenancy.repository.tenant.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:40
 * To change this template use File | Settings | File Templates.
 */
@Service
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationRepository organizationRepository;

    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public Optional<Organization> findById(Long id) {
        return organizationRepository.findById(id);
    }

    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public List<Organization> findAll() {
        return organizationRepository.findAll();
    }

    @Transactional("tenantTransactionManager")
    public Organization save(Organization organization) {
        return organizationRepository.save(organization);
    }

    @Transactional("tenantTransactionManager")
    public Organization update(Long id, Organization changes) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new TenantRecordNotFoundException("No organization with id " + id));
        organization.setName(changes.getName());
        organization.setAddress(changes.getAddress());
        organization.setEmail(changes.getEmail());
        return organization;
    }

    @Transactional("tenantTransactionManager")
    public void delete(Long id) {
        if (!organizationRepository.existsById(id)) {
            throw new TenantRecordNotFoundException("No organization with id " + id);
        }
        organizationRepository.deleteById(id);
    }
}
