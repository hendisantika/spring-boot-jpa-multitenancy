package id.my.hendisantika.multitenancy.repository.central;

import id.my.hendisantika.multitenancy.entity.central.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
