package in.codefarm.designarena.review;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/** Reads and writes evaluations. */
interface EvaluationRepository extends Repository<Evaluation, Long> {

    Evaluation save(Evaluation evaluation);

    /** The latest evaluation for a submission, with the submission loaded alongside it. */
    @Query("""
            SELECT e FROM Evaluation e
            JOIN FETCH e.submission s
            WHERE s.id = :submissionId
            ORDER BY e.createdAt DESC
            LIMIT 1
            """)
    Optional<Evaluation> findLatestBySubmissionId(long submissionId);
}
