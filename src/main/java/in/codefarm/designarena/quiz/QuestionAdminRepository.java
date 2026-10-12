package in.codefarm.designarena.quiz;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Reads and writes questions for the admin area.
 *
 * <p>Separate from {@link QuestionRepository}, which the quiz itself uses and which has
 * no write methods at all.
 */
interface QuestionAdminRepository extends Repository<Question, Long> {

    Question save(Question question);

    Optional<Question> findById(Long id);

    List<Question> findByProblemIdOrderByPositionAsc(long problemId);
}
