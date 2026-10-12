package in.codefarm.designarena.quiz;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a set of selected options into a score, a per-category breakdown and a
 * question-by-question explanation.
 *
 * <p><b>No language model is involved here, and none ever should be.</b> Scoring is a
 * rule applied to an answer key: it must be repeatable, explainable, and identical for
 * two people who answered the same way. Those are properties a model cannot promise. The
 * model's job on the results page is the narrative paragraph, nothing else.
 *
 * <p>The rules, stated once:
 * <ul>
 *   <li>One point per question. There is no partial credit and no weighting, so the score
 *       on the page is always "out of the number of questions".
 *   <li>A {@code SINGLE} question is correct when the one selected option is the correct
 *       one.
 *   <li>A {@code MULTI} question is correct when the selection matches the set of correct
 *       options exactly — every one of them, and nothing else. Partial credit would
 *       reward picking every option, which is the opposite of the judgement being tested.
 *   <li>An unanswered question scores zero and is reported as unanswered, which is not the
 *       same as answered wrongly.
 * </ul>
 *
 * <p>This class is deliberately free of Spring and JPA: it takes plain values and returns
 * plain values, so its tests need neither a container nor a database.
 */
final class QuizScorer {

    private QuizScorer() {
        // Static utility: scoring is a pure function, not an object with state.
    }

    /**
     * Scores one attempt.
     *
     * @param questions every question in the quiz, in display order
     * @param selectionsByQuestionId what the user selected, keyed by question id; a
     *     question missing from the map was not answered
     * @return the score, the category breakdown and a result per question
     */
    static ScoredAttempt score(List<ScorableQuestion> questions, Map<Long, Set<Long>> selectionsByQuestionId) {
        List<QuestionResult> results = new ArrayList<>(questions.size());
        Map<String, int[]> byCategory = new LinkedHashMap<>();
        int earned = 0;

        for (ScorableQuestion question : questions) {
            Set<Long> selected = selectionsByQuestionId.getOrDefault(question.id(), Set.of());
            Set<Long> correctIds = question.correctOptionIds();
            boolean answered = !selected.isEmpty();
            boolean correct = answered && selected.equals(correctIds);

            if (correct) {
                earned++;
            }
            int[] tally = byCategory.computeIfAbsent(question.category(), key -> new int[2]);
            tally[0] += correct ? 1 : 0;
            tally[1]++;

            results.add(new QuestionResult(
                    question.position(),
                    question.category(),
                    question.text(),
                    answered,
                    correct,
                    question.textOf(selected),
                    question.textOf(correctIds),
                    explain(question, selected, correct)));
        }

        List<CategoryScore> categories = byCategory.entrySet().stream()
                .map(entry -> new CategoryScore(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();

        return new ScoredAttempt(earned, questions.size(), categories, results);
    }

    /**
     * Builds the explanation shown under a question.
     *
     * <p>When the answer was right, the explanation of the correct option is enough. When
     * it was wrong, the user needs two things: why what they picked is wrong, and why the
     * right answer is right — in that order, because the first is the question they are
     * actually asking.
     */
    private static String explain(ScorableQuestion question, Set<Long> selected, boolean correct) {
        String whyCorrect = question.explanationOf(question.correctOptionIds());
        if (correct) {
            return whyCorrect;
        }
        String whyChosen = question.explanationOf(selected);
        return whyChosen.isEmpty() ? whyCorrect : whyChosen + " " + whyCorrect;
    }
}
