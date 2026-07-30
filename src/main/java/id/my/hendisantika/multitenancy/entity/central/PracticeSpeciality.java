package id.my.hendisantika.multitenancy.entity.central;

/**
 * What the organization practises, as picked on the registration form.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.28
 */
public enum PracticeSpeciality {

    GENERAL_PRACTICE("General Practice"),
    SPECIALIST_PRACTICE("Specialist Practice"),
    MULTIPLE_PRACTICES_MEDICAL_GROUP("Multiple Practices/Medical Group"),
    HOSPITAL("Hospital"),
    DENTAL("Dental"),
    AESTHETIC_AND_DERMA("Aesthetic & Derma"),
    ALLIED_HEALTH("Allied Health"),
    MENTAL_HEALTH("Mental Health"),
    OTHERS("Others");

    private final String label;

    PracticeSpeciality(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
