package id.my.hendisantika.multitenancy.entity.tenant;

import id.my.hendisantika.multitenancy.entity.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:34
 * To change this template use File | Settings | File Templates.
 */
@Data
@Entity
@Table(name = "organizations")
@EqualsAndHashCode(callSuper = false)
public class Organization extends BaseEntity {
    private static final long serialVersionUID = -6144389355317857388L;

    @Column(name = "name")
    private String name;

    /**
     * A {@code code} from the {@code UNIT_TYPE} list in reference_data, not its
     * label: renaming a label should not rewrite anybody's record.
     */
    @Column(name = "unit_type", length = 40)
    private String unitType;

    /**
     * A {@code code} from {@code OPERATING_STATUS}. A unit that has closed still
     * owns its records, so it gets a state rather than a deletion.
     */
    @Column(name = "operating_status", length = 40)
    private String operatingStatus;

    @Column(name = "address")
    private String address;

    /** A {@code code} from {@code PROVINCE}; the address itself stays free text. */
    @Column(name = "province", length = 40)
    private String province;

    @Column(name = "email")
    private String email;
}
