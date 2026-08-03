package id.my.hendisantika.multitenancy.repository.central;

import id.my.hendisantika.multitenancy.entity.central.EmailChange;
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
 * Date: 03/08/26
 * Time: 08.29
 */
@Repository
public interface EmailChangeRepository extends JpaRepository<EmailChange, Long> {

    Optional<EmailChange> findByTokenHash(String tokenHash);

    List<EmailChange> findAllByAccountId(Long accountId);
}
