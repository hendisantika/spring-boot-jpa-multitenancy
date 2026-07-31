package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.tenant.Person;
import id.my.hendisantika.multitenancy.repository.tenant.PersonRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public Optional<Person> findById(Long id) {
        return personRepository.findById(id);
    }

    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public List<Person> findAll() {
        return personRepository.findAll();
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
