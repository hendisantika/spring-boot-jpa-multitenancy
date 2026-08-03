package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.tenant.Person;
import id.my.hendisantika.multitenancy.service.storage.StorageService;

import java.time.LocalDate;
import java.time.ZoneId;
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
        LocalDate birthDate,
        String gender,
        String maritalStatus,
        String bloodType,
        String identityDocumentType,
        String identityNumber,
        String photoUrl,
        /*
         * The unit this person belongs to. The id is what a filter takes; the
         * name is what anybody reads, and a list that can be narrowed by unit
         * has to show which one, or the narrowing is invisible.
         */
        Long unitId,
        String unitName
) {

    public static PersonView of(Person person, StorageService storageService) {
        return new PersonView(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getEmail(),
                person.getMobile(),
                person.getHomePhone(),
                dateOf(person.getBirthDate()),
                person.getGender(),
                person.getMaritalStatus(),
                person.getBloodType(),
                person.getIdentityDocumentType(),
                person.getIdentityNumber(),
                storageService.urlOf(person.getPhotoKey()),
                person.getOrganization() == null ? null : person.getOrganization().getId(),
                person.getOrganization() == null ? null : person.getOrganization().getName());
    }

    /**
     * A birthday is a calendar date, not a moment, and it was being handed out
     * as one: midnight in the server's zone, serialised as an instant, which in
     * Jakarta is the previous day in UTC. Anything reading the first ten
     * characters — the edit form did — showed a birthday one day early, and
     * saving it back moved it again.
     */
    private static LocalDate dateOf(Date value) {
        if (value == null) {
            return null;
        }
        // java.sql.Date is what a DATE column comes back as, and it refuses
        // toInstant() because it has no time to offer.
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
