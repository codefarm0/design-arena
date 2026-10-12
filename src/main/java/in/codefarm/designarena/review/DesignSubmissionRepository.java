package in.codefarm.designarena.review;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Reads and writes submitted designs. */
interface DesignSubmissionRepository extends Repository<DesignSubmission, Long> {

    DesignSubmission save(DesignSubmission submission);

    Optional<DesignSubmission> findById(Long id);

    /**
     * How many designs this person submitted in a window.
     *
     * <p>Counted from the submissions themselves rather than from a running total kept on
     * the account. A counter has to be made correct across every failure path; this is
     * one indexed count on a small table, and it cannot drift.
     */
    long countByUserIdAndCreatedAtBetween(Long userId, Instant from, Instant to);

    /** Everything this person submitted, newest first, with the latest status of each. */
    @Query("""
            SELECT new in.codefarm.designarena.review.ReviewHistoryEntry(
                s.id, s.problemId, s.createdAt,
                (SELECT e.status FROM Evaluation e WHERE e.submission = s ORDER BY e.createdAt DESC LIMIT 1))
            FROM DesignSubmission s
            WHERE s.userId = :userId
            ORDER BY s.createdAt DESC
            """)
    List<ReviewHistoryEntry> findHistoryFor(long userId);
}
