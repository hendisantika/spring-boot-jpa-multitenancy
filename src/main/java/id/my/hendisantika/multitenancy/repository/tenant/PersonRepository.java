package id.my.hendisantika.multitenancy.repository.tenant;

import id.my.hendisantika.multitenancy.entity.tenant.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     */
    @Query("""
            select p from Person p
            where lower(coalesce(p.firstName, '')) like :term escape '\\'
               or lower(coalesce(p.lastName, '')) like :term escape '\\'
               or lower(concat(coalesce(p.firstName, ''), ' ', coalesce(p.lastName, ''))) like :term escape '\\'
               or lower(coalesce(p.email, '')) like :term escape '\\'
               or lower(coalesce(p.mobile, '')) like :term escape '\\'
            """)
    Page<Person> search(@Param("term") String term, Pageable pageable);
}
