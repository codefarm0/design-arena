package in.codefarm.designarena.quiz;

import java.util.List;

/**
 * The output of scoring, before the AI narrative is attached.
 *
 * @param score points earned
 * @param maxScore points available
 * @param categories the per-category breakdown
 * @param questions a result per question
 */
record ScoredAttempt(int score, int maxScore, List<CategoryScore> categories, List<QuestionResult> questions) {

    ScoredAttempt {
        categories = List.copyOf(categories);
        questions = List.copyOf(questions);
    }
}
