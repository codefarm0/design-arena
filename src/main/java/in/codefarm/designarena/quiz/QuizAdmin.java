package in.codefarm.designarena.quiz;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Editing the questions for a problem.
 *
 * <p>The validation here is the point of the class. A quiz question with no correct
 * option, or a single-choice question with two, cannot be scored fairly — and the
 * failure would show up as somebody losing a point they earned, long after the edit.
 * Checking at the moment of saving turns a silent content bug into a message.
 */
@Service
public class QuizAdmin {

    private final QuestionAdminRepository questions;

    QuizAdmin(QuestionAdminRepository questions) {
        this.questions = questions;
    }

    /** Every question for a problem, in order, ready to edit. */
    @Transactional(readOnly = true)
    public List<QuestionDraft> draftsFor(long problemId) {
        return questions.findByProblemIdOrderByPositionAsc(problemId).stream()
                .map(QuizAdmin::toDraft)
                .toList();
    }

    /**
     * Saves one question and its options.
     *
     * @throws InvalidQuestionException if the question could not be scored as written
     */
    @Transactional
    public void save(long problemId, QuestionDraft draft) {
        validate(draft);
        Question question = draft.id() == null
                ? new Question(problemId, draft.position())
                : questions
                        .findById(draft.id())
                        .orElseThrow(() -> new InvalidQuestionException("That question no longer exists"));

        question.apply(
                draft.position(),
                draft.text().strip(),
                draft.type(),
                draft.category().strip());

        List<QuestionOption> options = new ArrayList<>();
        for (int i = 0; i < draft.options().size(); i++) {
            OptionDraft option = draft.options().get(i);
            options.add(new QuestionOption(
                    question,
                    i + 1,
                    option.text().strip(),
                    option.correct(),
                    option.explanation().strip()));
        }
        question.replaceOptions(options);
        questions.save(question);
    }

    /**
     * The rules a question has to satisfy to be scoreable.
     *
     * <p>They mirror {@code QuizScorer} exactly: it awards the point when the selection
     * equals the set of correct options, so a question with none can never be right, and
     * a single-choice question with two correct options can never be right either.
     */
    private static void validate(QuestionDraft draft) {
        if (draft.text().isBlank()) {
            throw new InvalidQuestionException("The question needs some text");
        }
        if (draft.options().size() < 2) {
            throw new InvalidQuestionException("A question needs at least two options");
        }
        if (draft.options().stream().anyMatch(option -> option.text().isBlank())) {
            throw new InvalidQuestionException("Every option needs some text");
        }
        if (draft.options().stream().anyMatch(option -> option.explanation().isBlank())) {
            throw new InvalidQuestionException("Every option needs an explanation, including the wrong ones");
        }
        long correct = draft.correctCount();
        if (correct == 0) {
            throw new InvalidQuestionException("Mark at least one option correct, or nobody can score this question");
        }
        if (draft.type() == QuestionType.SINGLE && correct > 1) {
            throw new InvalidQuestionException("A single-choice question can only have one correct option");
        }
        if (draft.type() == QuestionType.MULTI && correct < 2) {
            throw new InvalidQuestionException("A multi-choice question needs at least two correct options");
        }
    }

    private static QuestionDraft toDraft(Question question) {
        List<OptionDraft> options = question.getOptions().stream()
                .map(option -> new OptionDraft(option.getText(), option.isCorrect(), option.getExplanation()))
                .toList();
        return new QuestionDraft(
                question.getId(),
                question.getPosition(),
                question.getText(),
                question.getType(),
                question.getCategory(),
                options);
    }
}
