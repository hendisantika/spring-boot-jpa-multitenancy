package id.my.hendisantika.multitenancy.repository.tenant;

import id.my.hendisantika.multitenancy.entity.tenant.Organization;
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
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    /**
     * A unit is found by whichever of its details somebody remembers: the name,
     * where it is, how to write to it, what kind of place it is, whether it is
     * open, or which province it sits in.
     * <p>
     * The free-text columns are matched against the term, which arrives
     * lower-cased, wildcard-wrapped and already escaped — hence the escape
     * clause. The three coded columns are matched against codes the caller has
     * already resolved from the labels, because the record stores
     * {@code BRANCH_CLINIC} and nobody types that.
     * <p>
     * A collection is never empty: the caller passes a sentinel that no code can
     * equal, so the clause is simply false rather than invalid SQL.
     */
    @Query("""
            select o from Organization o
            where (lower(coalesce(o.name, '')) like :term escape '\\'
                or lower(coalesce(o.address, '')) like :term escape '\\'
                or lower(coalesce(o.email, '')) like :term escape '\\'
                or o.unitType in :unitTypes
                or o.operatingStatus in :operatingStatuses
                or o.province in :provinces)
              and (:unitTypeIs is null or o.unitType = :unitTypeIs)
              and (:operatingStatusIs is null or o.operatingStatus = :operatingStatusIs)
              and (:provinceIs is null or o.province = :provinceIs)
            """)
    Page<Organization> search(@Param("term") String term,
                              @Param("unitTypes") Collection<String> unitTypes,
                              @Param("operatingStatuses") Collection<String> operatingStatuses,
                              @Param("provinces") Collection<String> provinces,
                              @Param("unitTypeIs") String unitTypeIs,
                              @Param("operatingStatusIs") String operatingStatusIs,
                              @Param("provinceIs") String provinceIs,
                              Pageable pageable);
}
