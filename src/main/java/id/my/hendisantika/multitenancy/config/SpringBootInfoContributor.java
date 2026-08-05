package id.my.hendisantika.multitenancy.config;

import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.SpringVersion;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds a "spring-boot" section to /actuator/info carrying the Spring Boot
 * version, and the Spring Framework version beside it.
 * <p>
 * The versions are read at runtime from the jars on the classpath rather than
 * stamped at build time, so they are whatever is actually running — no property
 * to keep in step with the parent POM.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 */
@Component
public class SpringBootInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, String> versions = new LinkedHashMap<>();
        // getVersion() reads the implementation version from the jar manifest and
        // can be null if that is stripped; fall back rather than omit the field.
        versions.put("version", orUnknown(SpringBootVersion.getVersion()));
        versions.put("spring-framework-version", orUnknown(SpringVersion.getVersion()));
        builder.withDetail("spring-boot", versions);
    }

    private String orUnknown(String version) {
        return version != null ? version : "unknown";
    }
}
