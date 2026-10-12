package in.codefarm.designarena.quiz;

import in.codefarm.designarena.TestcontainersConfiguration;
import in.codefarm.designarena.ai.AiReviewer;
import in.codefarm.designarena.ai.QuizSummaryRequest;
import in.codefarm.designarena.problem.ProblemCatalogue;
import in.codefarm.designarena.user.Accounts;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The quiz flow end to end against a real Postgres and the real seed data.
 *
 * <p>The model is the only thing stubbed. Scoring, persistence and the queries are all
 * real, because those are what the test is about — and because a stubbed repository would
 * only prove that the stub works.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    TestcontainersConfiguration.class,
    QuizService.class,
    QuizAttemptStore.class,
    ProblemCatalogue.class,
    Accounts.class,
    QuizServiceTest.TestBeans.class
})
class QuizServiceTest {

    private final QuizService quiz;
    private final RecordingReviewer reviewer;
    private final JdbcTemplate jdbc;

    QuizServiceTest(QuizService quiz, AiReviewer reviewer, JdbcTemplate jdbc) {
        this.quiz = quiz;
        this.reviewer = (RecordingReviewer) reviewer;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void resetTheStub() {
        // The stub is a singleton in a cached Spring context, so without this its call
        // count and its silent flag would leak between tests and make them order-dependent.
        reviewer.reset();
    }

    @Test
    void worksWithoutAnAccount() {
        // Answering ten multiple-choice questions should not require signing up. An
        // anonymous attempt is owned by nobody and so never appears in a history.
        QuizSession session = quiz.start("rate-limiter");

        assertThat(session.attemptId()).isPositive();
        assertThat(jdbc.queryForObject(
                        "SELECT user_id FROM quiz_attempt WHERE id = ?", Long.class, session.attemptId()))
                .isNull();
    }

    @Test
    void startsAnAttemptWithEverySeededQuestion() {
        QuizSession session = quiz.start("rate-limiter");

        assertThat(session.attemptId()).isPositive();
        assertThat(session.problemTitle()).isEqualTo("Design a Rate Limiter");
        assertThat(session.questions()).hasSize(10);
        assertThat(session.questions().getFirst().options()).hasSize(4);
        assertThat(session.questions().getFirst().options().getFirst().key()).isEqualTo("A");
    }

    @Test
    void neverSendsTheAnswerKeyToTheBrowser() {
        // Adding "correct" or "explanation" to QuizOption would put the answers in the
        // page before the user submits. This test is here so that change fails loudly.
        assertThat(QuizOption.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly("id", "key", "text");
    }

    @Test
    void scoresOnlyWhatWasAnsweredCorrectly() {
        QuizSession session = quiz.start("rate-limiter");
        answerCorrectly(session, 0);
        answerIncorrectly(session, 1);

        QuizResults results = quiz.submit(session.attemptId());

        assertThat(results.score()).isEqualTo(1);
        assertThat(results.maxScore()).isEqualTo(10);
        assertThat(results.questions()).hasSize(10);
        assertThat(results.questions().getFirst().correct()).isTrue();
        assertThat(results.questions().get(1).correct()).isFalse();
        assertThat(results.questions().get(2).answered()).isFalse();
    }

    @Test
    void replacesAnEarlierAnswerToTheSameQuestion() {
        QuizSession session = quiz.start("rate-limiter");
        answerIncorrectly(session, 0);
        answerCorrectly(session, 0);

        assertThat(quiz.submit(session.attemptId()).score()).isEqualTo(1);
    }

    @Test
    void breaksTheScoreDownByTheSeededCategories() {
        QuizSession session = quiz.start("rate-limiter");
        answerCorrectly(session, 0);

        QuizResults results = quiz.submit(session.attemptId());

        assertThat(results.categories())
                .extracting(CategoryScore::category)
                .contains("ALGORITHM", "CONSISTENCY", "FAILURE HANDLING");
        assertThat(results.categories().stream()
                        .mapToInt(CategoryScore::maxScore)
                        .sum())
                .isEqualTo(10);
    }

    @Test
    void storesTheNarrativeForTheResultsPageToPickUp() {
        QuizSession session = quiz.start("rate-limiter");
        answerCorrectly(session, 0);

        quiz.submit(session.attemptId());

        // submit() does not wait for the model, so the narrative is read back rather than
        // returned. With the inline executor above it is already stored by now.
        assertThat(quiz.results(session.attemptId()).summary()).contains(RecordingReviewer.NARRATIVE);
    }

    @Test
    void returnsTheScoreWithoutWaitingForTheModel() {
        QuizSession session = quiz.start("rate-limiter");
        answerCorrectly(session, 0);

        QuizResults immediate = quiz.submit(session.attemptId());

        assertThat(immediate.score()).isEqualTo(1);
        assertThat(immediate.questions()).hasSize(10);
    }

    @Test
    void doesNotAskTheModelAgainForAnAttemptThatAlreadyHasASummary() {
        QuizSession session = quiz.start("rate-limiter");
        answerCorrectly(session, 0);
        quiz.submit(session.attemptId());
        int callsAfterFirstSubmit = reviewer.calls();

        quiz.submit(session.attemptId());

        assertThat(callsAfterFirstSubmit).isEqualTo(1);
        assertThat(reviewer.calls()).isEqualTo(callsAfterFirstSubmit);
    }

    @Test
    void sendsTheModelTheDecisionsRatherThanTheRawAnswerIds() {
        QuizSession session = quiz.start("rate-limiter");
        answerCorrectly(session, 0);
        quiz.submit(session.attemptId());

        QuizSummaryRequest sent = reviewer.lastRequest();
        assertThat(sent.problemTitle()).isEqualTo("Design a Rate Limiter");
        assertThat(sent.score()).isEqualTo(1);
        assertThat(sent.maxScore()).isEqualTo(10);
        assertThat(sent.decisions())
                .hasSize(10)
                .allSatisfy(line -> assertThat(line).contains("chose:"));
    }

    @Test
    void stillProducesFullResultsWhenTheModelSaysNothing() {
        reviewer.goSilent();
        QuizSession session = quiz.start("rate-limiter");
        answerCorrectly(session, 0);

        QuizResults results = quiz.submit(session.attemptId());

        assertThat(results.summary()).isEmpty();
        assertThat(results.score()).isEqualTo(1);
        assertThat(results.questions()).hasSize(10);
        assertThat(results.categories()).isNotEmpty();
        assertThat(results.questions().getFirst().explanation()).isNotBlank();
    }

    @Test
    void refusesToRecordAnAnswerAfterSubmission() {
        QuizSession session = quiz.start("rate-limiter");
        long attemptId = session.attemptId();
        quiz.submit(attemptId);
        QuizQuestion question = session.questions().getFirst();
        Set<Long> selection = Set.of(question.options().getFirst().id());

        assertThatThrownBy(() -> quiz.answer(attemptId, question.id(), selection))
                .isInstanceOf(QuizAlreadySubmittedException.class);
    }

    @Test
    void rejectsAnUnknownAttempt() {
        assertThatThrownBy(() -> quiz.results(999_999L)).isInstanceOf(QuizNotFoundException.class);
    }

    private void answerCorrectly(QuizSession session, int questionIndex) {
        QuizQuestion question = session.questions().get(questionIndex);
        quiz.answer(session.attemptId(), question.id(), correctOptionIds(question));
    }

    private void answerIncorrectly(QuizSession session, int questionIndex) {
        QuizQuestion question = session.questions().get(questionIndex);
        Set<Long> correct = correctOptionIds(question);
        long wrong = question.options().stream()
                .map(QuizOption::id)
                .filter(id -> !correct.contains(id))
                .findFirst()
                .orElseThrow();
        quiz.answer(session.attemptId(), question.id(), Set.of(wrong));
    }

    /** The answer key, read straight from the seeded data rather than from the API. */
    private Set<Long> correctOptionIds(QuizQuestion question) {
        return new HashSet<>(jdbc.queryForList(
                "SELECT id FROM question_option WHERE question_id = ? AND is_correct", Long.class, question.id()));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        /** Fixed, so nothing in these tests depends on how long they take to run. */
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        AiReviewer aiReviewer() {
            return new RecordingReviewer();
        }

        /** Counters are recorded in production; a simple registry collects them here. */
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        /** Accounts needs one, even though no quiz test signs in. */
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        /**
         * Runs background work on the calling thread. The production executor is a real
         * pool; here, running inline keeps every assertion deterministic without a sleep
         * or an awaitility loop.
         */
        @Bean
        Executor summaryExecutor() {
            return Runnable::run;
        }
    }

    /** A model that always answers the same way, and can be told to stop answering. */
    static final class RecordingReviewer implements AiReviewer {

        static final String NARRATIVE = "You chose a shared counter and favoured availability.";

        private QuizSummaryRequest lastRequest;
        private int calls;
        private boolean silent;

        @Override
        public Optional<String> summariseQuizAttempt(QuizSummaryRequest request) {
            this.lastRequest = request;
            this.calls++;
            return silent ? Optional.empty() : Optional.of(NARRATIVE);
        }

        void goSilent() {
            this.silent = true;
        }

        @Override
        public in.codefarm.designarena.ai.AiReviewResult reviewDesign(
                in.codefarm.designarena.ai.DesignReviewRequest request) {
            throw new UnsupportedOperationException("The quiz never reviews a design");
        }

        @Override
        public in.codefarm.designarena.ai.AiStatus status() {
            return new in.codefarm.designarena.ai.AiStatus("stub", "stub-model", true, 2, java.util.Map.of());
        }

        void reset() {
            this.lastRequest = null;
            this.calls = 0;
            this.silent = false;
        }

        int calls() {
            return calls;
        }

        QuizSummaryRequest lastRequest() {
            return lastRequest;
        }
    }
}
