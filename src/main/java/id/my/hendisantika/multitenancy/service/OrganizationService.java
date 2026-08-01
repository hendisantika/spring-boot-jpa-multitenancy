package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.tenant.Organization;
import id.my.hendisantika.multitenancy.repository.tenant.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    private final ReferenceDataService referenceDataService;

    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public Optional<Organization> findById(Long id) {
        return organizationRepository.findById(id);
    }

    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public List<Organization> findAll() {
        return organizationRepository.findAll(TenantListing.ORDER);
    }

    /**
     * Same paging rules as the people list, from the same place, so the two
     * screens cannot drift apart in how they clamp or how they escape.
     * <p>
     * The search reaches the coded fields as well as the free-text ones, by
     * resolving what was typed to codes first: somebody looking for the Bali
     * branch types "Bali", not {@code BALI}.
     * <p>
     * Filters narrow on top of it. One query serves every combination, because
     * a term that matches everything stands in for "nothing was searched for".
     */
    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public Page<Organization> findPage(String query, UnitFilter filter, Integer page, Integer size) {
        Pageable pageable = TenantListing.pageRequest(page, size);
        String term = TenantListing.searchTerm(query);
        return organizationRepository.search(
                term == null ? TenantListing.MATCH_EVERYTHING : term,
                referenceDataService.codesForSearch("UNIT_TYPE", query),
                referenceDataService.codesForSearch("OPERATING_STATUS", query),
                referenceDataService.codesForSearch("PROVINCE", query),
                filter.unitType(),
                filter.operatingStatus(),
                filter.anyProvince(),
                TenantListing.orNothing(filter.provinces()),
                pageable);
    }

    @Transactional("tenantTransactionManager")
    public Organization save(Organization organization) {
        normaliseCodes(organization);
        return organizationRepository.save(organization);
    }

    @Transactional("tenantTransactionManager")
    public Organization update(Long id, Organization changes) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new TenantRecordNotFoundException("No organization with id " + id));
        normaliseCodes(changes);
        organization.setName(changes.getName());
        organization.setUnitType(changes.getUnitType());
        organization.setOperatingStatus(changes.getOperatingStatus());
        organization.setAddress(changes.getAddress());
        organization.setProvince(changes.getProvince());
        organization.setEmail(changes.getEmail());
        return organization;
    }

    /**
     * The same rule as on a person: the form offers a dropdown, but the request
     * can be sent without one, so the code is checked where it is stored.
     */
    private void normaliseCodes(Organization organization) {
        organization.setUnitType(referenceDataService.requireValidCode(
                "UNIT_TYPE", organization.getUnitType(), "unit type"));
        organization.setOperatingStatus(referenceDataService.requireValidCode(
                "OPERATING_STATUS", organization.getOperatingStatus(), "operating status"));
        organization.setProvince(referenceDataService.requireValidCode(
                "PROVINCE", organization.getProvince(), "province"));
    }

    @Transactional("tenantTransactionManager")
    public void delete(Long id) {
        if (!organizationRepository.existsById(id)) {
            throw new TenantRecordNotFoundException("No organization with id " + id);
        }
        organizationRepository.deleteById(id);
    }
}
