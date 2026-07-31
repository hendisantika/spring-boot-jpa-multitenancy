package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.tenant.Person;
import id.my.hendisantika.multitenancy.service.PersonFilter;
import id.my.hendisantika.multitenancy.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * People inside whichever tenant the request resolved to.
 * <p>
 * A MEMBER may read and write them: that is the daily work of a clinic, and
 * withholding it would leave the role useless. Only an OWNER may delete, because
 * a removed record is not something a shift should be able to undo by mistake.
 * <p>
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

    @GetMapping("/person/{id}")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public Person getPerson(@PathVariable("id") Long id) {
        return personService.findById(id).orElse(null);
    }

    /**
     * Paged rather than whole: a tenant's list of people only grows, and a
     * request that returns all of it is one nobody can withdraw later.
     *
     * A search widens and a filter narrows, so they combine: {@code ?q=budi} with
     * {@code &bloodType=O_POSITIVE} means both, not either.
     *
     * @param q    matched against the names, email, mobile and the labels behind the codes
     * @param page zero based
     * @param size clamped, so a client cannot ask for the lot in one go
     */
    @GetMapping("/person")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public PageResponse<Person> listPeople(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "gender", required = false) String gender,
            @RequestParam(name = "maritalStatus", required = false) String maritalStatus,
            @RequestParam(name = "bloodType", required = false) String bloodType,
            @RequestParam(name = "identityDocumentType", required = false) String identityDocumentType) {
        PersonFilter filter = PersonFilter.of(gender, maritalStatus, bloodType, identityDocumentType);
        return PageResponse.of(personService.findPage(q, filter, page, size));
    }

    @PostMapping("/person")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public ResponseEntity<Person> createPerson(@Valid @RequestBody Person person) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personService.save(person));
    }

    @PutMapping("/person/{id}")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public Person updatePerson(@PathVariable("id") Long id, @Valid @RequestBody Person person) {
        return personService.update(id, person);
    }

    @DeleteMapping("/person/{id}")
    @PreAuthorize("@tenantSecurity.isOwnerOfCurrentTenant()")
    public ResponseEntity<Void> deletePerson(@PathVariable("id") Long id) {
        personService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
