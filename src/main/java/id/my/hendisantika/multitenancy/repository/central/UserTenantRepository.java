package id.my.hendisantika.multitenancy.repository.central;

import id.my.hendisantika.multitenancy.entity.central.TenantRole;
import id.my.hendisantika.multitenancy.entity.central.UserTenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Repository
public interface UserTenantRepository extends JpaRepository<UserTenant, Long> {

    List<UserTenant> findAllByAccountId(Long accountId);

    List<UserTenant> findAllByTenantSlug(String tenantSlug);

    Page<UserTenant> findAllByTenantSlug(String tenantSlug, Pageable pageable);

    /**
     * The memberships of one tenant whose address or role matches what somebody
     * typed. A search widens, so the two are an OR.
     * <p>
     * The address comes from the account rather than from
     * {@link UserTenant#getUserName()}: that column holds the address the
     * membership was granted to, which is not where the account may since have
     * moved. Searching what is shown and showing what is current is the same
     * decision, made in both places.
     * <p>
     * Roles arrive already resolved. Matching them here would mean casting an
     * enum to text in HQL; the tenant lists resolve their codes in Java for the
     * same reason, and "own" has to find the owners either way — nobody types
     * OWNER.
     */
    @Query("""
            select m from UserTenant m
            where m.tenantSlug = :tenantSlug
              and (lower(coalesce(m.account.email, '')) like :term escape '\\'
                or lower(coalesce(m.userName, '')) like :term escape '\\'
                or m.role in :roles)
            """)
    Page<UserTenant> search(@Param("tenantSlug") String tenantSlug,
                            @Param("term") String term,
                            @Param("roles") Collection<TenantRole> roles,
                            Pageable pageable);

    /**
     * One membership by the pair that identifies it. Queried rather than found
     * by walking the list, which stopped being the same thing once the list
     * became a page.
     */
    Optional<UserTenant> findByTenantSlugAndAccountId(String tenantSlug, Long accountId);

    boolean existsByAccountIdAndTenantSlug(Long accountId, String tenantSlug);
}
