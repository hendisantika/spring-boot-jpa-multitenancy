package id.my.hendisantika.multitenancy.repository.central;

import id.my.hendisantika.multitenancy.entity.central.Invitation;
import id.my.hendisantika.multitenancy.entity.central.InvitationStatus;
import id.my.hendisantika.multitenancy.entity.central.TenantRole;
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
 * Time: 09.14
 */
@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    List<Invitation> findAllByTenantSlugAndStatusOrderByCreatedAtDesc(String tenantSlug, InvitationStatus status);

    List<Invitation> findAllByTenantSlug(String tenantSlug);

    /**
     * A page of one tenant's invitations in a given state, narrowed to what
     * somebody typed. A search widens, so the address and the role are an OR,
     * and the roles arrive already resolved for the reason the membership
     * search resolves its own: matching an enum as text in HQL means casting it.
     */
    @Query("""
            select i from Invitation i
            where i.tenantSlug = :tenantSlug
              and i.status = :status
              and (lower(i.email) like :term escape '\\'
                or i.role in :roles)
            """)
    Page<Invitation> search(@Param("tenantSlug") String tenantSlug,
                            @Param("status") InvitationStatus status,
                            @Param("term") String term,
                            @Param("roles") Collection<TenantRole> roles,
                            Pageable pageable);

    Optional<Invitation> findFirstByTenantSlugAndEmailIgnoreCaseAndStatus(
            String tenantSlug, String email, InvitationStatus status);
}
