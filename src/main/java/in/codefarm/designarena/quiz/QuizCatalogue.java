package in.codefarm.designarena.quiz;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/** What the rest of the application may ask about the quizzes that exist. */
@Service
public class QuizCatalogue {

    private final QuestionRepository questions;

    QuizCatalogue(QuestionRepository questions) {
        this.questions = questions;
    }

    /**
     * How many questions each problem has, keyed by problem id.
     *
     * <p>A problem with no questions is absent from the map rather than present with a
     * zero, because the query groups over existing rows. Callers should treat a missing
     * key as none.
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> questionCountsByProblemId() {
        return questions.countQuestionsPerProblem().stream()
                .collect(Collectors.toMap(ProblemQuestionCount::problemId, ProblemQuestionCount::total));
    }
}
