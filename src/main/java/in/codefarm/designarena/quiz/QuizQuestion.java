package in.codefarm.designarena.quiz;

import java.util.List;

/**
 * One question, as the UI needs it.
 *
 * @param id the question's identity
 * @param position its place in the quiz, counting from one
 * @param text the question
 * @param type whether one option or several must be selected
 * @param category the rubric category, shown as a chip and scored separately
 * @param options the options in display order
 */
public record QuizQuestion(
        long id, int position, String text, QuestionType type, String category, List<QuizOption> options) {

    public QuizQuestion {
        options = List.copyOf(options);
    }
}
