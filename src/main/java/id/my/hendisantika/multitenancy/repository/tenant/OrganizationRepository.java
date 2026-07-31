package id.my.hendisantika.multitenancy.repository.tenant;

import id.my.hendisantika.multitenancy.entity.tenant.Organization;
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
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    /**
     * A unit is found by whichever of its three details somebody remembers: the
     * name, where it is, or how to write to it. The term arrives lower-cased,
     * wildcard-wrapped and already escaped, which is what the escape clause is
     * for.
     */
    @Query("""
            select o from Organization o
            where lower(coalesce(o.name, '')) like :term escape '\\'
               or lower(coalesce(o.address, '')) like :term escape '\\'
               or lower(coalesce(o.email, '')) like :term escape '\\'
            """)
    Page<Organization> search(@Param("term") String term, Pageable pageable);
}
