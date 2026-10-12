package in.codefarm.designarena.quiz;

import java.util.List;

/**
 * The editable shape of a question.
 *
 * @param id the question being edited, or empty for a new one
 * @param position its place in the quiz
 * @param text the question
 * @param type whether one option or several are correct
 * @param category the rubric category it scores under
 * @param options the options, in display order
 */
public record QuestionDraft(
        Long id, int position, String text, QuestionType type, String category, List<OptionDraft> options) {

    public QuestionDraft {
        options = List.copyOf(options);
    }

    /** How many options are marked correct, which decides whether the type is consistent. */
    public long correctCount() {
        return options.stream().filter(OptionDraft::correct).count();
    }
}
