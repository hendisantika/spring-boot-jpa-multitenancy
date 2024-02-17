package id.my.hendisantika.multitenancy.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:43
 * To change this template use File | Settings | File Templates.
 */
@Getter
public class DatabaseConfiguration {
    @Value("${application.database.url}")
    private String url;

    @Value("${application.database.user}")
    private String user;

    @Value("${application.database.password}")
    private String password;
}
