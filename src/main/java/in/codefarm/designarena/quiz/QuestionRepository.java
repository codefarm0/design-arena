package in.codefarm.designarena.quiz;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

/** Reads questions. */
interface QuestionRepository extends Repository<Question, Long> {

    /**
     * Question totals for every problem, as a single grouped query.
     *
     * <p>One aggregate rather than a count per problem: the catalogue renders every problem
     * at once, so counting per card would be an N+1 against the question table.
     */
    @Query("""
            SELECT new in.codefarm.designarena.quiz.ProblemQuestionCount(q.problemId, COUNT(q))
            FROM Question q
            GROUP BY q.problemId
            """)
    List<ProblemQuestionCount> countQuestionsPerProblem();

    /**
     * Every question for a problem, with its options, in display order.
     *
     * <p>{@code LEFT JOIN FETCH} loads the options in the same query. Without it, rendering
     * ten questions would issue ten more queries for their options.
     */
    @Query("""
            SELECT DISTINCT q FROM Question q
            LEFT JOIN FETCH q.options
            WHERE q.problemId = :problemId
            ORDER BY q.position
            """)
    List<Question> findByProblemIdWithOptions(long problemId);
}
