package in.codefarm.designarena.quiz;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** Reads and writes quiz attempts. */
interface QuizAttemptRepository extends Repository<QuizAttempt, Long> {

    QuizAttempt save(QuizAttempt attempt);

    Optional<QuizAttempt> findById(Long id);

    /** An attempt with its answers already loaded, for scoring and for the results page. */
    @Query("""
            SELECT DISTINCT a FROM QuizAttempt a
            LEFT JOIN FETCH a.answers
            WHERE a.id = :id
            """)
    Optional<QuizAttempt> findByIdWithAnswers(long id);

    /**
     * This person's submitted attempts, newest first.
     *
     * <p>Unsubmitted attempts are left out on purpose: an abandoned quiz is not history,
     * it is a tab somebody closed.
     */
    @Query("""
            SELECT new in.codefarm.designarena.quiz.QuizHistoryEntry(a.id, a.problemId, a.submittedAt, a.score, a.maxScore)
            FROM QuizAttempt a
            WHERE a.userId = :userId AND a.submittedAt IS NOT NULL
            ORDER BY a.submittedAt DESC
            """)
    List<QuizHistoryEntry> findHistoryFor(long userId);
}
