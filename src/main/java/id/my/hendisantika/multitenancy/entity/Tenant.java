package id.my.hendisantika.multitenancy.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.Getter;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:36
 * To change this template use File | Settings | File Templates.
 */
@Getter
public enum Tenant {
    DEFAULT(1, "default", "System default database"),
    ORGTEST1(2, "orgtest1", "Sample database for organization 1"),
    ORGTEST2(3, "orgtest2", "Sample database for organization 2");

    private final Integer id;
    private final String name;
    private final String description;

    Tenant(Integer id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static Tenant findByName(String name) {
        return Arrays.stream(Tenant.values())
                .filter(t -> t.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(DEFAULT);
    }

    public static Tenant findById(Integer id) {
        return Arrays.stream(Tenant.values())
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(DEFAULT);
    }

    @Converter
    public static class TypeConverter implements AttributeConverter<Tenant, Integer> {
        @Override
        public Integer convertToDatabaseColumn(Tenant tenantName) {
            return tenantName != null ? tenantName.getId() : null;
        }

        @Override
        public Tenant convertToEntityAttribute(Integer id) {
            return id != null ? Tenant.findById(id) : null;
        }
    }
}
