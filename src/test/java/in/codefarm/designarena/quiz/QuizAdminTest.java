package in.codefarm.designarena.quiz;

import in.codefarm.designarena.TestcontainersConfiguration;
import in.codefarm.designarena.problem.ProblemCatalogue;
import in.codefarm.designarena.problem.ProblemSummary;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Editing questions, and refusing the ones that could not be scored.
 *
 * <p>The validation rules exist to mirror {@link QuizScorer}: anything this class accepts
 * must be answerable in a way that can earn the point. These tests are the place that
 * link is checked.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, QuizAdmin.class, ProblemCatalogue.class})
class QuizAdminTest {

    private final QuizAdmin admin;
    private final long problemId;

    QuizAdminTest(QuizAdmin admin, ProblemCatalogue catalogue) {
        this.admin = admin;
        this.problemId = catalogue.activeProblems().stream()
                .filter(problem -> problem.slug().equals("rate-limiter"))
                .map(ProblemSummary::id)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void readsTheSeededQuestionsBackForEditing() {
        List<QuestionDraft> drafts = admin.draftsFor(problemId);

        assertThat(drafts).hasSize(10);
        assertThat(drafts.getFirst().options()).hasSize(4);
        assertThat(drafts.getFirst().correctCount()).isEqualTo(1);
        assertThat(drafts.getFirst().options())
                .allSatisfy(option -> assertThat(option.explanation()).isNotBlank());
    }

    @Test
    void addsANewQuestion() {
        admin.save(problemId, draft(null, 11, QuestionType.SINGLE, option("Right", true), option("Wrong", false)));

        List<QuestionDraft> drafts = admin.draftsFor(problemId);
        assertThat(drafts).hasSize(11);
        assertThat(drafts.getLast().text()).isEqualTo("A new question");
    }

    @Test
    void editsAnExistingQuestionWithoutDuplicatingIt() {
        QuestionDraft existing = admin.draftsFor(problemId).getFirst();

        admin.save(
                problemId,
                new QuestionDraft(
                        existing.id(),
                        existing.position(),
                        "Edited question text",
                        existing.type(),
                        existing.category(),
                        existing.options()));

        List<QuestionDraft> drafts = admin.draftsFor(problemId);
        assertThat(drafts).hasSize(10);
        assertThat(drafts.getFirst().text()).isEqualTo("Edited question text");
    }

    @Test
    void replacesTheOptionsRatherThanAppending() {
        QuestionDraft existing = admin.draftsFor(problemId).getFirst();

        admin.save(
                problemId,
                new QuestionDraft(
                        existing.id(),
                        existing.position(),
                        existing.text(),
                        QuestionType.SINGLE,
                        existing.category(),
                        List.of(option("Only right", true), option("Only wrong", false))));

        assertThat(admin.draftsFor(problemId).getFirst().options()).hasSize(2);
    }

    @Test
    void refusesAQuestionWithNoCorrectOption() {
        assertThatThrownBy(() -> admin.save(
                        problemId, draft(null, 11, QuestionType.SINGLE, option("A", false), option("B", false))))
                .isInstanceOf(InvalidQuestionException.class)
                .hasMessageContaining("at least one option correct");
    }

    @Test
    void refusesASingleChoiceQuestionWithTwoCorrectOptions() {
        // QuizScorer awards the point when the selection equals the correct set, so a
        // single-choice question with two correct answers can never be answered correctly.
        assertThatThrownBy(() -> admin.save(
                        problemId, draft(null, 11, QuestionType.SINGLE, option("A", true), option("B", true))))
                .isInstanceOf(InvalidQuestionException.class)
                .hasMessageContaining("only have one correct option");
    }

    @Test
    void refusesAMultiChoiceQuestionWithOneCorrectOption() {
        assertThatThrownBy(() -> admin.save(
                        problemId, draft(null, 11, QuestionType.MULTI, option("A", true), option("B", false))))
                .isInstanceOf(InvalidQuestionException.class)
                .hasMessageContaining("at least two correct options");
    }

    @Test
    void refusesAnOptionWithoutAnExplanation() {
        assertThatThrownBy(() -> admin.save(
                        problemId,
                        draft(
                                null,
                                11,
                                QuestionType.SINGLE,
                                new OptionDraft("Right", true, ""),
                                option("Wrong", false))))
                .isInstanceOf(InvalidQuestionException.class)
                .hasMessageContaining("explanation");
    }

    @Test
    void refusesAQuestionWithASingleOption() {
        assertThatThrownBy(() -> admin.save(problemId, draft(null, 11, QuestionType.SINGLE, option("Only one", true))))
                .isInstanceOf(InvalidQuestionException.class)
                .hasMessageContaining("at least two options");
    }

    @Test
    void refusesAQuestionWithNoText() {
        assertThatThrownBy(() -> admin.save(
                        problemId,
                        new QuestionDraft(
                                null,
                                11,
                                "  ",
                                QuestionType.SINGLE,
                                "SCALING",
                                List.of(option("A", true), option("B", false)))))
                .isInstanceOf(InvalidQuestionException.class)
                .hasMessageContaining("needs some text");
    }

    @Test
    void leavesTheQuizUnchangedWhenAnEditIsRefused() {
        QuestionDraft existing = admin.draftsFor(problemId).getFirst();
        String originalText = existing.text();

        assertThatThrownBy(() -> admin.save(
                        problemId,
                        new QuestionDraft(
                                existing.id(),
                                existing.position(),
                                "Would be fine",
                                QuestionType.SINGLE,
                                existing.category(),
                                List.of(option("A", true), option("B", true)))))
                .isInstanceOf(InvalidQuestionException.class);

        assertThat(admin.draftsFor(problemId).getFirst().text()).isEqualTo(originalText);
    }

    private static QuestionDraft draft(Long id, int position, QuestionType type, OptionDraft... options) {
        return new QuestionDraft(id, position, "A new question", type, "SCALING", List.of(options));
    }

    private static OptionDraft option(String text, boolean correct) {
        return new OptionDraft(text, correct, "Because of how the limiter counts requests.");
    }
}
