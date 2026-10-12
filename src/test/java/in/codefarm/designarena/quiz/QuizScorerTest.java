package in.codefarm.designarena.quiz;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scoring is the one part of the results page that must be exactly right every time, so it
 * is a pure function and these tests are plain JUnit: no Spring context, no database.
 */
class QuizScorerTest {

    private static final ScorableQuestion SINGLE = new ScorableQuestion(
            1L,
            1,
            "Which algorithm allows short bursts?",
            "ALGORITHM",
            List.of(
                    new ScorableOption(10L, "Fixed window", false, "Allows a double burst at the boundary."),
                    new ScorableOption(11L, "Token bucket", true, "Saved tokens absorb a burst.")));

    private static final ScorableQuestion MULTI = new ScorableQuestion(
            2L,
            2,
            "Which headers help a client stay inside its limit?",
            "API DESIGN",
            List.of(
                    new ScorableOption(20L, "Retry-After", true, "Says when to try again."),
                    new ScorableOption(21L, "Remaining quota", true, "Lets a client slow down early."),
                    new ScorableOption(22L, "Internal counter key", false, "Leaks implementation detail.")));

    @Nested
    @DisplayName("single-choice questions")
    class SingleChoice {

        @Test
        void earnsThePointWhenTheCorrectOptionIsSelected() {
            ScoredAttempt scored = QuizScorer.score(List.of(SINGLE), Map.of(1L, Set.of(11L)));

            assertThat(scored.score()).isEqualTo(1);
            assertThat(scored.maxScore()).isEqualTo(1);
            assertThat(scored.questions().getFirst().correct()).isTrue();
        }

        @Test
        void earnsNothingWhenTheWrongOptionIsSelected() {
            ScoredAttempt scored = QuizScorer.score(List.of(SINGLE), Map.of(1L, Set.of(10L)));

            assertThat(scored.score()).isZero();
            assertThat(scored.questions().getFirst().correct()).isFalse();
        }
    }

    @Nested
    @DisplayName("multi-choice questions")
    class MultiChoice {

        @Test
        void earnsThePointOnlyWhenEveryCorrectOptionIsSelected() {
            ScoredAttempt scored = QuizScorer.score(List.of(MULTI), Map.of(2L, Set.of(20L, 21L)));

            assertThat(scored.score()).isEqualTo(1);
        }

        @Test
        void earnsNothingForASubsetOfTheCorrectOptions() {
            ScoredAttempt scored = QuizScorer.score(List.of(MULTI), Map.of(2L, Set.of(20L)));

            assertThat(scored.score()).isZero();
        }

        @Test
        void earnsNothingWhenAWrongOptionIsIncludedAlongsideTheCorrectOnes() {
            // Selecting everything must not score, or the honest answer is to select everything.
            ScoredAttempt scored = QuizScorer.score(List.of(MULTI), Map.of(2L, Set.of(20L, 21L, 22L)));

            assertThat(scored.score()).isZero();
        }
    }

    @Test
    void treatsAnUnansweredQuestionAsUnansweredRatherThanWrong() {
        ScoredAttempt scored = QuizScorer.score(List.of(SINGLE), Map.of());

        QuestionResult result = scored.questions().getFirst();
        assertThat(result.answered()).isFalse();
        assertThat(result.correct()).isFalse();
        assertThat(result.yourAnswers()).isEmpty();
        assertThat(scored.score()).isZero();
    }

    @Test
    void scoresOutOfTheNumberOfQuestionsRegardlessOfHowManyWereAnswered() {
        ScoredAttempt scored = QuizScorer.score(List.of(SINGLE, MULTI), Map.of(1L, Set.of(11L)));

        assertThat(scored.score()).isEqualTo(1);
        assertThat(scored.maxScore()).isEqualTo(2);
    }

    @Test
    void breaksTheScoreDownByCategory() {
        ScoredAttempt scored = QuizScorer.score(List.of(SINGLE, MULTI), Map.of(1L, Set.of(11L), 2L, Set.of(20L)));

        assertThat(scored.categories())
                .containsExactly(new CategoryScore("ALGORITHM", 1, 1), new CategoryScore("API DESIGN", 0, 1));
        assertThat(scored.categories().getFirst().percentage()).isEqualTo(100);
    }

    @Test
    void explainsOnlyTheRightAnswerWhenTheUserWasRight() {
        ScoredAttempt scored = QuizScorer.score(List.of(SINGLE), Map.of(1L, Set.of(11L)));

        assertThat(scored.questions().getFirst().explanation()).isEqualTo("Saved tokens absorb a burst.");
    }

    @Test
    void explainsTheChosenOptionBeforeTheRightOneWhenTheUserWasWrong() {
        ScoredAttempt scored = QuizScorer.score(List.of(SINGLE), Map.of(1L, Set.of(10L)));

        QuestionResult result = scored.questions().getFirst();
        assertThat(result.explanation())
                .isEqualTo("Allows a double burst at the boundary. Saved tokens absorb a burst.");
        assertThat(result.yourAnswers()).containsExactly("Fixed window");
        assertThat(result.correctAnswers()).containsExactly("Token bucket");
    }

    @Test
    void explainsTheRightAnswerWhenTheQuestionWasSkipped() {
        ScoredAttempt scored = QuizScorer.score(List.of(SINGLE), Map.of());

        assertThat(scored.questions().getFirst().explanation()).isEqualTo("Saved tokens absorb a burst.");
    }
}
