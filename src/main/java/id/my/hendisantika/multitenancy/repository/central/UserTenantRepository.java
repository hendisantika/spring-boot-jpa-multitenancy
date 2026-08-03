package id.my.hendisantika.multitenancy.repository.central;

import id.my.hendisantika.multitenancy.entity.central.UserTenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * Time: 06.09
 */
@Repository
public interface UserTenantRepository extends JpaRepository<UserTenant, Long> {

    List<UserTenant> findAllByAccountId(Long accountId);

    List<UserTenant> findAllByTenantSlug(String tenantSlug);

    Page<UserTenant> findAllByTenantSlug(String tenantSlug, Pageable pageable);

    /**
     * One membership by the pair that identifies it. Queried rather than found
     * by walking the list, which stopped being the same thing once the list
     * became a page.
     */
    Optional<UserTenant> findByTenantSlugAndAccountId(String tenantSlug, Long accountId);

    boolean existsByAccountIdAndTenantSlug(Long accountId, String tenantSlug);
}
