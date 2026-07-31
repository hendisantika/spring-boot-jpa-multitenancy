package id.my.hendisantika.multitenancy.entity.tenant;

import id.my.hendisantika.multitenancy.entity.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A value from one of the lists every tenant starts with: genders, blood types,
 * appointment statuses and so on.
 * <p>
 * The same rows are seeded into every tenant database rather than kept centrally
 * and shared, because a tenant that cannot add its own visit type has a list it
 * cannot use. {@code systemDefined} is what tells the two apart.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 18.32
 */
@Getter
@Setter
@Entity
@Table(name = "reference_data")
public class ReferenceData extends BaseEntity {

    private static final long serialVersionUID = -8891547729164451103L;

    /** Which list this belongs to, such as {@code BLOOD_TYPE}. */
    @Column(name = "category", nullable = false, length = 40)
    private String category;

    /** Stable across renames, so stored data does not depend on the wording. */
    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /**
     * A value that is no longer offered but still appears in old records, which
     * is why it is switched off rather than deleted.
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /** Seeded by a migration rather than added by the tenant. */
    @Column(name = "system_defined", nullable = false)
    private Boolean systemDefined = false;
}
