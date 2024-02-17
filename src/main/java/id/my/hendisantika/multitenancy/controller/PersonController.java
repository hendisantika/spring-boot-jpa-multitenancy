package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.Person;
import id.my.hendisantika.multitenancy.service.PersonService;
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
 * Time: 13:58
 * To change this template use File | Settings | File Templates.
 */
@RestController
@RequiredArgsConstructor
public class PersonController {
    private final PersonService personService;

    @GetMapping(value = "/person/{id}")
    public Person getPerson(@PathVariable("id") Long id) {
        Optional<Person> person = personService.findById(id);
        return person.orElse(null);
    }
}
