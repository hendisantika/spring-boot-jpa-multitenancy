package id.my.hendisantika.multitenancy.repository.central;

import id.my.hendisantika.multitenancy.entity.central.Invitation;
import id.my.hendisantika.multitenancy.entity.central.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

    Optional<Invitation> findFirstByTenantSlugAndEmailIgnoreCaseAndStatus(
            String tenantSlug, String email, InvitationStatus status);
}
