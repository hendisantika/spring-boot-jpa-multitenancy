package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.Person;
import id.my.hendisantika.multitenancy.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
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

    public Optional<Person> findById(Long id) {
        return personRepository.findById(id);
    }
}
