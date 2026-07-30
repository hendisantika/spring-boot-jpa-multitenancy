package id.my.hendisantika.multitenancy.entity.central;

/**
 * How the organization is laid out, as picked on the registration form.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.28
 */
public enum OrgStructure {

    SINGLE_LOCATION_CLINIC("Single Location Clinic"),
    MULTI_LOCATION_CLINIC("Multi Location Clinic"),
    SINGLE_LOCATION_HOSPITAL("Single Location Hospital"),
    MULTI_LOCATION_HOSPITAL("Multi Location Hospital");

    private final String label;

    OrgStructure(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
