package in.codefarm.designarena.quiz;

import java.util.List;
import java.util.Optional;

/**
 * Everything the results page renders.
 *
 * <p>The summary is optional on purpose. It is the only part produced by a language model,
 * and the page is required to render completely without it.
 *
 * @param attemptId the attempt these results belong to
 * @param problemSlug the problem's URL identifier
 * @param problemTitle the problem's title
 * @param score points earned
 * @param maxScore points available, one per question
 * @param categories the per-category breakdown
 * @param questions every question, with the user's answer and the explanation
 * @param summary the AI narrative, absent when the model did not answer
 */
public record QuizResults(
        long attemptId,
        String problemSlug,
        String problemTitle,
        int score,
        int maxScore,
        List<CategoryScore> categories,
        List<QuestionResult> questions,
        Optional<String> summary) {

    public QuizResults {
        categories = List.copyOf(categories);
        questions = List.copyOf(questions);
    }
}
