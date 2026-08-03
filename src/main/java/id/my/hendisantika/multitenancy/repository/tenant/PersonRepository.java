package id.my.hendisantika.multitenancy.repository.tenant;

import id.my.hendisantika.multitenancy.entity.tenant.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:39
 * To change this template use File | Settings | File Templates.
 */
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    /**
     * Someone searching for "budi santoso" is typing a name, not a column, so
     * the first and last name are matched joined as well as apart. The term
     * arrives lower-cased, wildcard-wrapped and with any {@code %} the user
     * typed already escaped, which is what the escape clause is for.
     * <p>
     * The four coded columns are matched against codes the caller has already
     * resolved from the labels, because the record stores {@code O_POSITIVE} and
     * nobody types that. A collection is never empty: the caller passes a
     * sentinel that no code can equal, so the clause is simply false rather than
     * invalid SQL.
     * <p>
     * Every filter takes several values at once, so each is switched off by its
     * own flag rather than by a null. Within one the values mean either; against
     * the other filters they still mean both.
     * <p>
     * The unit is a record rather than a code, so it filters by id — but it
     * searches by name, because that is what somebody types. A person with no
     * unit is matched by neither, which is what "the people at Braga" means.
     * <p>
     * The join is explicitly a LEFT one. Writing {@code p.organization.name}
     * instead makes it an inner join, which silently drops everybody who has no
     * unit from the whole list — not just from the unit filter.
     */
    @Query("""
            select p from Person p
            left join p.organization u
            where (lower(coalesce(p.firstName, '')) like :term escape '\\'
                or lower(coalesce(p.lastName, '')) like :term escape '\\'
                or lower(concat(coalesce(p.firstName, ''), ' ', coalesce(p.lastName, ''))) like :term escape '\\'
                or lower(coalesce(p.email, '')) like :term escape '\\'
                or lower(coalesce(p.mobile, '')) like :term escape '\\'
                or lower(coalesce(u.name, '')) like :term escape '\\'
                or p.gender in :genders
                or p.maritalStatus in :maritalStatuses
                or p.bloodType in :bloodTypes
                or p.identityDocumentType in :identityDocuments)
              and (:anyGender = true or p.gender in :genderIn)
              and (:anyMaritalStatus = true or p.maritalStatus in :maritalStatusIn)
              and (:anyBloodType = true or p.bloodType in :bloodTypeIn)
              and (:anyIdentityDocument = true or p.identityDocumentType in :identityDocumentIn)
              and (:anyUnit = true or u.id in :unitIn)
            """)
    Page<Person> search(@Param("term") String term,
                        @Param("genders") Collection<String> genders,
                        @Param("maritalStatuses") Collection<String> maritalStatuses,
                        @Param("bloodTypes") Collection<String> bloodTypes,
                        @Param("identityDocuments") Collection<String> identityDocuments,
                        @Param("anyGender") boolean anyGender,
                        @Param("genderIn") Collection<String> genderIn,
                        @Param("anyMaritalStatus") boolean anyMaritalStatus,
                        @Param("maritalStatusIn") Collection<String> maritalStatusIn,
                        @Param("anyBloodType") boolean anyBloodType,
                        @Param("bloodTypeIn") Collection<String> bloodTypeIn,
                        @Param("anyIdentityDocument") boolean anyIdentityDocument,
                        @Param("identityDocumentIn") Collection<String> identityDocumentIn,
                        @Param("anyUnit") boolean anyUnit,
                        @Param("unitIn") Collection<Long> unitIn,
                        Pageable pageable);
}
