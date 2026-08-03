package id.my.hendisantika.multitenancy.repository.tenant;

import id.my.hendisantika.multitenancy.entity.tenant.ReferenceData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    /**
     * Every value this tenant keeps, across the lists, narrowed to what
     * somebody typed and to the categories and states they asked for.
     * <p>
     * One table is what this always was: a category is a column, not a
     * separate list, so filtering by it is a filter like any other. The
     * per-category query above is the same thing with the category fixed by
     * the path.
     * <p>
     * The state is two flags rather than a nullable Boolean: binding null to a
     * parameter that is also compared makes Hibernate guess at its type, and
     * it guesses wrong.
     */
    @Query("""
            select r from ReferenceData r
            where (lower(r.label) like :term escape '\\'
                or lower(r.code) like :term escape '\\'
                or lower(r.category) like :term escape '\\')
              and (:anyCategory = true or r.category in :categoryIn)
              and (:anyState = true or r.active = :active)
            """)
    Page<ReferenceData> searchAll(@Param("term") String term,
                                  @Param("anyCategory") boolean anyCategory,
                                  @Param("categoryIn") Collection<String> categoryIn,
                                  @Param("anyState") boolean anyState,
                                  @Param("active") boolean active,
                                  Pageable pageable);

    /** The categories this tenant keeps, for offering them as a filter. */
    @Query("select distinct r.category from ReferenceData r order by r.category")
    List<String> findCategories();
}
