package id.my.hendisantika.multitenancy.repository.central;

import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.entity.central.TenantStatus;
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
public interface TenantRegistrationRepository extends JpaRepository<TenantRegistration, Long> {

    Optional<TenantRegistration> findBySlug(String slug);

    Optional<TenantRegistration> findBySubdomain(String subdomain);

    List<TenantRegistration> findAllByStatus(TenantStatus status);

    boolean existsBySlugOrDatabaseName(String slug, String databaseName);
}
