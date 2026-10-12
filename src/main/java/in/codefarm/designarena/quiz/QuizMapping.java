package in.codefarm.designarena.quiz;

import java.util.List;

/** Turns question entities into the records the rest of the application sees. */
final class QuizMapping {

    private QuizMapping() {
        // Static mapping helpers.
    }

    /** The view of a question that goes to the browser: no correctness, no explanations. */
    static QuizQuestion toQuizQuestion(Question question) {
        List<QuizOption> options = question.getOptions().stream()
                .map(option -> new QuizOption(option.getId(), letterFor(option.getPosition()), option.getText()))
                .toList();
        return new QuizQuestion(
                question.getId(),
                question.getPosition(),
                question.getText(),
                question.getType(),
                question.getCategory(),
                options);
    }

    /** The view of a question that scoring needs: the answer key and the explanations. */
    static ScorableQuestion toScorable(Question question) {
        List<ScorableOption> options = question.getOptions().stream()
                .map(option -> new ScorableOption(
                        option.getId(), option.getText(), option.isCorrect(), option.getExplanation()))
                .toList();
        return new ScorableQuestion(
                question.getId(), question.getPosition(), question.getText(), question.getCategory(), options);
    }

    /** A, B, C... from a one-based position. */
    private static String letterFor(int position) {
        return String.valueOf((char) ('A' + position - 1));
    }
}
