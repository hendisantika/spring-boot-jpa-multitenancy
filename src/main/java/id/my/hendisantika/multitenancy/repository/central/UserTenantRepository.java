package id.my.hendisantika.multitenancy.repository.central;

import id.my.hendisantika.multitenancy.entity.central.UserTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    boolean existsByAccountIdAndTenantSlug(Long accountId, String tenantSlug);
}
