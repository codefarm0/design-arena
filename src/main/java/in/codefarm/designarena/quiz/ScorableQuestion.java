package in.codefarm.designarena.quiz;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A question reduced to what scoring needs: the answer key and the text to quote back.
 *
 * <p>Separate from the {@code Question} entity so that {@link QuizScorer} depends on plain
 * values rather than on JPA, and so its tests can build a case in two lines.
 *
 * @param id the question's identity
 * @param position its place in the quiz
 * @param text the question
 * @param category the rubric category
 * @param options every option, with its correctness and explanation
 */
record ScorableQuestion(long id, int position, String text, String category, List<ScorableOption> options) {

    ScorableQuestion {
        options = List.copyOf(options);
    }

    Set<Long> correctOptionIds() {
        return options.stream()
                .filter(ScorableOption::correct)
                .map(ScorableOption::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** The text of the given options, in display order. */
    List<String> textOf(Set<Long> optionIds) {
        return options.stream()
                .filter(option -> optionIds.contains(option.id()))
                .map(ScorableOption::text)
                .toList();
    }

    /** The explanations of the given options, in display order, joined into one paragraph. */
    String explanationOf(Set<Long> optionIds) {
        return options.stream()
                .filter(option -> optionIds.contains(option.id()))
                .map(ScorableOption::explanation)
                .collect(Collectors.joining(" "));
    }
}
