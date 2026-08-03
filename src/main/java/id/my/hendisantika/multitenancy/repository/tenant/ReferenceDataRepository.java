package id.my.hendisantika.multitenancy.repository.tenant;

import id.my.hendisantika.multitenancy.entity.tenant.ReferenceData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 18.36
 */
@Repository
public interface ReferenceDataRepository extends JpaRepository<ReferenceData, Long> {

    List<ReferenceData> findAllByOrderByCategoryAscSortOrderAsc();

    List<ReferenceData> findByCategoryOrderBySortOrderAsc(String category);

    List<ReferenceData> findByCategoryAndActiveTrueOrderBySortOrderAsc(String category);

    /**
     * Whether this tenant keeps the list at all, asked without the search so
     * that "nothing matched" and "no such list" stay different answers.
     */
    boolean existsByCategory(String category);

    /**
     * One category, a page at a time, narrowed to what somebody typed.
     * <p>
     * The label and the code both match here, unlike everywhere else. Elsewhere
     * a code is storage and never shown, so searching it would answer questions
     * nobody asked; on the screen this feeds, the code is a column — and what
     * is on screen is what a search should reach.
     */
    @Query("""
            select r from ReferenceData r
            where r.category = :category
              and (lower(r.label) like :term escape '\\'
                or lower(r.code) like :term escape '\\')
            """)
    Page<ReferenceData> search(@Param("category") String category,
                               @Param("term") String term,
                               Pageable pageable);
}
