package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.tenant.Person;
import id.my.hendisantika.multitenancy.service.PersonFilter;
import id.my.hendisantika.multitenancy.service.PersonService;
import id.my.hendisantika.multitenancy.service.TenantRecordNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import id.my.hendisantika.multitenancy.service.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    private static final String PHOTO_PREFIX = "persons";

    private final PersonService personService;

    private final StorageService storageService;

    /**
     * One person, for the screen that shows the whole record.
     * <p>
     * Missing is 404, not 200 with an empty body: a detail screen has to tell
     * "no such person" apart from "the API is unreachable", and it could not.
     */
    @GetMapping("/person/{id}")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public PersonView getPerson(@PathVariable("id") Long id) {
        return personService.findById(id).map(this::viewOf)
                .orElseThrow(() -> new TenantRecordNotFoundException("No person with id " + id));
    }

    /**
     * Paged rather than whole: a tenant's list of people only grows, and a
     * request that returns all of it is one nobody can withdraw later.
     *
     * A search widens and a filter narrows, so they combine: {@code ?q=budi} with
     * {@code &bloodType=O_POSITIVE} means both, not either.
     * <p>
     * Every filter may be repeated —
     * {@code ?bloodType=O_POSITIVE&bloodType=O_NEGATIVE} — and then means either
     * of them, while still narrowing whatever else was asked for.
     *
     * @param q    matched against the names, email, mobile and the labels behind the codes
     * @param page zero based
     * @param size clamped, so a client cannot ask for the lot in one go
     */
    @GetMapping("/person")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public PageResponse<PersonView> listPeople(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "gender", required = false) List<String> gender,
            @RequestParam(name = "maritalStatus", required = false) List<String> maritalStatus,
            @RequestParam(name = "bloodType", required = false) List<String> bloodType,
            @RequestParam(name = "identityDocumentType", required = false) List<String> identityDocumentType) {
        PersonFilter filter = PersonFilter.of(gender, maritalStatus, bloodType, identityDocumentType);
        return PageResponse.of(personService.findPage(q, filter, page, size).map(this::viewOf));
    }

    @PostMapping("/person")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public ResponseEntity<PersonView> createPerson(@Valid @RequestBody Person person) {
        return ResponseEntity.status(HttpStatus.CREATED).body(viewOf(personService.save(person)));
    }

    /**
     * The same thing with a photo attached.
     * <p>
     * A second mapping rather than turning the JSON one into multipart: that
     * would break every caller that already posts JSON for a record with no
     * photo, which is most of them. The shape matches the organization
     * endpoints — a JSON part named after the record, plus an optional photo.
     */
    @PostMapping(path = "/person", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public ResponseEntity<PersonView> createPersonWithPhoto(
            @Valid @RequestPart("person") Person person,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(viewOf(personService.save(person, keyOf(photo))));
    }

    @PutMapping("/person/{id}")
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public PersonView updatePerson(@PathVariable("id") Long id, @Valid @RequestBody Person person) {
        return viewOf(personService.update(id, person));
    }

    /**
     * Omitting the photo part keeps the current one; sending one replaces it and
     * the old object is removed; {@code removePhoto=true} drops it entirely.
     * <p>
     * Sending both a photo and the flag is a contradiction, and the upload wins
     * — choosing a file says more than ticking a box, and the screen does not
     * let the two happen together anyway.
     */
    @PutMapping(path = "/person/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@tenantSecurity.isMemberOfCurrentTenant()")
    public PersonView updatePersonWithPhoto(
            @PathVariable("id") Long id,
            @Valid @RequestPart("person") Person person,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "removePhoto", defaultValue = "false") boolean removePhoto) {
        return viewOf(personService.update(id, person, keyOf(photo), removePhoto));
    }

    private String keyOf(MultipartFile photo) {
        return photo != null && !photo.isEmpty() ? storageService.store(photo, PHOTO_PREFIX) : null;
    }

    private PersonView viewOf(Person person) {
        return PersonView.of(person, storageService);
    }

    @DeleteMapping("/person/{id}")
    @PreAuthorize("@tenantSecurity.isOwnerOfCurrentTenant()")
    public ResponseEntity<Void> deletePerson(@PathVariable("id") Long id) {
        personService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
