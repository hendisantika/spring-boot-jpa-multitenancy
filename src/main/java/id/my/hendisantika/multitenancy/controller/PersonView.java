package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.tenant.Person;
import id.my.hendisantika.multitenancy.service.storage.StorageService;

import java.util.Date;

/**
 * A person as a client sees one.
 * <p>
 * The entity is no longer handed out directly: it carries {@code photoKey},
 * which is storage rather than something a client should ever hold, and what a
 * client needs instead is a signed URL that has to be built per response.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 03/08/26
 * Time: 07.34
 */
public record PersonView(
        Long id,
        String firstName,
        String lastName,
        String email,
        String mobile,
        String homePhone,
        Date birthDate,
        String gender,
        String maritalStatus,
        String bloodType,
        String identityDocumentType,
        String identityNumber,
        String photoUrl
) {

    public static PersonView of(Person person, StorageService storageService) {
        return new PersonView(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getEmail(),
                person.getMobile(),
                person.getHomePhone(),
                person.getBirthDate(),
                person.getGender(),
                person.getMaritalStatus(),
                person.getBloodType(),
                person.getIdentityDocumentType(),
                person.getIdentityNumber(),
                storageService.urlOf(person.getPhotoKey()));
    }
}
