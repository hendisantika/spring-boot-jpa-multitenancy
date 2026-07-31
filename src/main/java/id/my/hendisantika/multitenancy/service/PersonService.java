package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.tenant.Person;
import id.my.hendisantika.multitenancy.repository.tenant.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business data of whichever tenant the request resolved to. Nothing here names
 * a tenant: Hibernate routes the session, so the same code serves every one.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:41
 * To change this template use File | Settings | File Templates.
 */
@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    private final ReferenceDataService referenceDataService;

    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public Optional<Person> findById(Long id) {
        return personRepository.findById(id);
    }

    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public List<Person> findAll() {
        return personRepository.findAll(TenantListing.ORDER);
    }

    /**
     * A blank search is not a search: it means "everybody", so it skips the
     * query with all those LIKEs in it rather than matching them all against
     * "%%".
     * <p>
     * The search reaches the coded fields as well as the free-text ones, by
     * resolving what was typed to codes first: somebody looking for a blood
     * type types "O+", not {@code O_POSITIVE}. Same as a unit's, from the same
     * place, so the two screens cannot answer differently.
     * <p>
     * Filters narrow on top of it. One query serves every combination — search
     * with no filter, filter with no search, both, neither — because a term
     * that matches everything stands in for "nothing was searched for".
     */
    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public Page<Person> findPage(String query, PersonFilter filter, Integer page, Integer size) {
        Pageable pageable = TenantListing.pageRequest(page, size);
        String term = TenantListing.searchTerm(query);
        return personRepository.search(
                term == null ? TenantListing.MATCH_EVERYTHING : term,
                referenceDataService.codesForSearch("GENDER", query),
                referenceDataService.codesForSearch("MARITAL_STATUS", query),
                referenceDataService.codesForSearch("BLOOD_TYPE", query),
                referenceDataService.codesForSearch("IDENTITY_DOCUMENT", query),
                filter.gender(),
                filter.maritalStatus(),
                filter.bloodType(),
                filter.identityDocumentType(),
                pageable);
    }

    @Transactional("tenantTransactionManager")
    public Person save(Person person) {
        normaliseCodes(person);
        return personRepository.save(person);
    }

    @Transactional("tenantTransactionManager")
    public Person update(Long id, Person changes) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new TenantRecordNotFoundException("No person with id " + id));
        normaliseCodes(changes);
        person.setFirstName(changes.getFirstName());
        person.setLastName(changes.getLastName());
        person.setEmail(changes.getEmail());
        person.setMobile(changes.getMobile());
        person.setHomePhone(changes.getHomePhone());
        person.setBirthDate(changes.getBirthDate());
        person.setGender(changes.getGender());
        person.setMaritalStatus(changes.getMaritalStatus());
        person.setBloodType(changes.getBloodType());
        person.setIdentityDocumentType(changes.getIdentityDocumentType());
        person.setIdentityNumber(changes.getIdentityNumber());
        return person;
    }

    /**
     * The four fields the form offers as dropdowns hold codes from the tenant's
     * own reference lists. The form is a courtesy; this is the rule, because the
     * same request can be sent without it.
     */
    private void normaliseCodes(Person person) {
        person.setGender(referenceDataService.requireValidCode("GENDER", person.getGender(), "gender"));
        person.setMaritalStatus(referenceDataService.requireValidCode(
                "MARITAL_STATUS", person.getMaritalStatus(), "marital status"));
        person.setBloodType(referenceDataService.requireValidCode(
                "BLOOD_TYPE", person.getBloodType(), "blood type"));
        person.setIdentityDocumentType(referenceDataService.requireValidCode(
                "IDENTITY_DOCUMENT", person.getIdentityDocumentType(), "identity document"));
    }

    @Transactional("tenantTransactionManager")
    public void delete(Long id) {
        if (!personRepository.existsById(id)) {
            throw new TenantRecordNotFoundException("No person with id " + id);
        }
        personRepository.deleteById(id);
    }
}
