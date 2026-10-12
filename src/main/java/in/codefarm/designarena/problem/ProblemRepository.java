package in.codefarm.designarena.problem;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Reads problems.
 *
 * <p>Extends the bare {@link Repository} marker rather than {@code JpaRepository}, so the
 * only methods that exist are the ones declared here. There is no inherited
 * {@code findAll()} to reach for, and no save or delete until a feature needs one.
 */
interface ProblemRepository extends Repository<Problem, Long> {

    /**
     * Active problems in catalogue order, projected straight into the record the UI needs.
     *
     * <p>The projection is part of the query so the database returns five columns rather
     * than whole entities that would then have to be mapped and discarded.
     */
    @Query("""
            SELECT new in.codefarm.designarena.problem.ProblemSummary(p.id, p.slug, p.title, p.difficulty, p.blurb)
            FROM Problem p
            WHERE p.active = TRUE
            ORDER BY p.position
            """)
    List<ProblemSummary> findActiveSummaries();

    /** One active problem by its URL identifier, projected the same way. */
    @Query("""
            SELECT new in.codefarm.designarena.problem.ProblemSummary(p.id, p.slug, p.title, p.difficulty, p.blurb)
            FROM Problem p
            WHERE p.slug = :slug AND p.active = TRUE
            """)
    Optional<ProblemSummary> findActiveSummaryBySlug(String slug);

    /** One problem by identity, for features that hold only the id. */
    @Query("""
            SELECT new in.codefarm.designarena.problem.ProblemSummary(p.id, p.slug, p.title, p.difficulty, p.blurb)
            FROM Problem p
            WHERE p.id = :id
            """)
    Optional<ProblemSummary> findSummaryById(long id);

    /**
     * The whole problem, for the design editor.
     *
     * <p>The requirement and scale-hint collections are left lazy and read inside the
     * service's transaction. Fetching both in one query would multiply the rows together;
     * two small follow-up selects for a single problem is the cheaper and clearer trade.
     */
    Optional<Problem> findBySlugAndActiveIsTrue(String slug);
}
