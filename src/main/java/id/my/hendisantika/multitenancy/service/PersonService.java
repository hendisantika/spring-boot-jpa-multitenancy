package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.tenant.Person;
import id.my.hendisantika.multitenancy.repository.tenant.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    /**
     * A page is a promise that asking for the next one shows different records,
     * which an unordered query cannot keep. Insertion order is the least
     * surprising of the deterministic ones.
     */
    private static final Sort ORDER = Sort.by(Sort.Direction.ASC, "id");

    /**
     * High enough that nobody paging through a clinic notices it, low enough
     * that one request cannot ask for the whole database.
     */
    static final int MAX_PAGE_SIZE = 200;

    static final int DEFAULT_PAGE_SIZE = 20;

    private final PersonRepository personRepository;

    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public Optional<Person> findById(Long id) {
        return personRepository.findById(id);
    }

    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public List<Person> findAll() {
        return personRepository.findAll(ORDER);
    }

    /**
     * A blank search is not a search: it means "everybody", so it skips the
     * query with five LIKEs in it rather than matching them all against "%%".
     */
    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public Page<Person> findPage(String query, Integer page, Integer size) {
        Pageable pageable = pageRequest(page, size);
        String term = searchTerm(query);
        return term == null ? personRepository.findAll(pageable) : personRepository.search(term, pageable);
    }

    private Pageable pageRequest(Integer page, Integer size) {
        int number = page == null ? 0 : Math.max(0, page);
        int length = size == null ? DEFAULT_PAGE_SIZE : Math.clamp(size, 1, MAX_PAGE_SIZE);
        return PageRequest.of(number, length, ORDER);
    }

    /**
     * The wildcards a user types are part of their name, not part of the query,
     * so they are escaped rather than honoured.
     */
    private String searchTerm(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String escaped = query.strip()
                .toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    @Transactional("tenantTransactionManager")
    public Person save(Person person) {
        return personRepository.save(person);
    }

    @Transactional("tenantTransactionManager")
    public Person update(Long id, Person changes) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new TenantRecordNotFoundException("No person with id " + id));
        person.setFirstName(changes.getFirstName());
        person.setLastName(changes.getLastName());
        person.setEmail(changes.getEmail());
        person.setMobile(changes.getMobile());
        person.setHomePhone(changes.getHomePhone());
        person.setBirthDate(changes.getBirthDate());
        person.setSocialSecurityNumber(changes.getSocialSecurityNumber());
        return person;
    }

    @Transactional("tenantTransactionManager")
    public void delete(Long id) {
        if (!personRepository.existsById(id)) {
            throw new TenantRecordNotFoundException("No person with id " + id);
        }
        personRepository.deleteById(id);
    }
}
