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
     * Blood type and identity document filter on several values at once, so each
     * is switched off by its own flag rather than by a null. Within one the
     * values mean either; against the other filters they still mean both.
     */
    @Query("""
            select p from Person p
            where (lower(coalesce(p.firstName, '')) like :term escape '\\'
                or lower(coalesce(p.lastName, '')) like :term escape '\\'
                or lower(concat(coalesce(p.firstName, ''), ' ', coalesce(p.lastName, ''))) like :term escape '\\'
                or lower(coalesce(p.email, '')) like :term escape '\\'
                or lower(coalesce(p.mobile, '')) like :term escape '\\'
                or p.gender in :genders
                or p.maritalStatus in :maritalStatuses
                or p.bloodType in :bloodTypes
                or p.identityDocumentType in :identityDocuments)
              and (:genderIs is null or p.gender = :genderIs)
              and (:maritalStatusIs is null or p.maritalStatus = :maritalStatusIs)
              and (:anyBloodType = true or p.bloodType in :bloodTypeIn)
              and (:anyIdentityDocument = true or p.identityDocumentType in :identityDocumentIn)
            """)
    Page<Person> search(@Param("term") String term,
                        @Param("genders") Collection<String> genders,
                        @Param("maritalStatuses") Collection<String> maritalStatuses,
                        @Param("bloodTypes") Collection<String> bloodTypes,
                        @Param("identityDocuments") Collection<String> identityDocuments,
                        @Param("genderIs") String genderIs,
                        @Param("maritalStatusIs") String maritalStatusIs,
                        @Param("anyBloodType") boolean anyBloodType,
                        @Param("bloodTypeIn") Collection<String> bloodTypeIn,
                        @Param("anyIdentityDocument") boolean anyIdentityDocument,
                        @Param("identityDocumentIn") Collection<String> identityDocumentIn,
                        Pageable pageable);
}
