package id.my.hendisantika.multitenancy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:44
 * To change this template use File | Settings | File Templates.
 */
@Configuration
@EnableJpaRepositories(basePackages = {"id.my.hendisantika.multitenancy.repository"})
@EnableSpringConfigured
@EnableTransactionManagement
public class RepositoryConfiguration {
}
