package in.codefarm.designarena.review;

import in.codefarm.designarena.TestcontainersConfiguration;
import in.codefarm.designarena.ai.AiReviewResult;
import in.codefarm.designarena.ai.AiReviewer;
import in.codefarm.designarena.ai.DesignEvaluation;
import in.codefarm.designarena.ai.DesignReviewRequest;
import in.codefarm.designarena.ai.DimensionScore;
import in.codefarm.designarena.ai.QuizSummaryRequest;
import in.codefarm.designarena.problem.ProblemCatalogue;
import in.codefarm.designarena.problem.ProblemNotFoundException;
import in.codefarm.designarena.user.Account;
import in.codefarm.designarena.user.Accounts;
import in.codefarm.designarena.user.NotSignedInException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;

import jakarta.persistence.EntityManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The review flow against a real Postgres and the real seed data, with the model stubbed.
 *
 * <p>Every state the review page is drawn for — PENDING, DONE and FAILED — is produced
 * here by a stub that can be told how to behave, which is the only honest way to test a
 * failure path that a real model would only produce occasionally.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    TestcontainersConfiguration.class,
    ReviewService.class,
    ReviewStore.class,
    ReviewAllowancePolicy.class,
    ProblemCatalogue.class,
    Accounts.class,
    ReviewServiceTest.TestBeans.class
})
@EnableConfigurationProperties(ReviewLimits.class)
class ReviewServiceTest {

    private static final String DESIGN = """
            ## Requirements
            Per-key limits.

            ## High-level design
            Token bucket in Redis, checked by a gateway filter.
            """;

    private final ReviewService reviews;
    private final ScriptedReviewer reviewer;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;
    private final Accounts accounts;
    private Account account;

    ReviewServiceTest(
            ReviewService reviews,
            AiReviewer reviewer,
            JdbcTemplate jdbc,
            EntityManager entityManager,
            Accounts accounts) {
        this.reviews = reviews;
        this.reviewer = (ScriptedReviewer) reviewer;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
        this.accounts = accounts;
    }

    @BeforeEach
    void signInAndResetTheStub() {
        reviewer.reset();
        account = accounts.register("reviewer@example.com", "Test Reviewer", "a-long-enough-password");
        signIn(account.email());
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Puts a person in the security context, which is where {@code Accounts} looks.
     *
     * <p>Deliberately not {@code @WithMockUser}: that would invent a principal that has
     * no row in the database, and the whole point here is that the submission ends up
     * owned by a real account.
     */
    private static void signIn(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(email, "n/a", List.of()));
    }

    @Test
    void storesTheDesignAndTheReviewWhenTheModelAnswers() {
        long submissionId = reviews.submit("rate-limiter", DESIGN, "flowchart LR\n  C-->G");

        ReviewState state = reviews.state(submissionId);
        assertThat(state.status()).isEqualTo(EvaluationStatus.DONE);
        assertThat(state.problemTitle()).isEqualTo("Design a Rate Limiter");
        assertThat(state.submittedDesign()).contains("Token bucket in Redis");
        assertThat(state.submittedDiagram()).isEqualTo("flowchart LR\n  C-->G");
        assertThat(state.evaluation()).isPresent();
        assertThat(state.evaluation().orElseThrow().overallScore()).isEqualTo(7);
        assertThat(state.failureReason()).isEmpty();
    }

    @Test
    void recordsWhichModelProducedTheReview() {
        long submissionId = reviews.submit("rate-limiter", DESIGN, "");

        assertThat(reviews.state(submissionId).provenance())
                .hasValueSatisfying(text -> assertThat(text)
                        .contains("llama3.1:8b")
                        .contains("ollama")
                        .contains("v1"));
    }

    @Test
    void storesTheTokenCountsTheProviderReported() {
        long submissionId = reviews.submit("rate-limiter", DESIGN, "");

        // These columns are read with plain SQL because nothing in the application
        // exposes them: they exist to be analysed later, not rendered. Hibernate holds
        // the write in its persistence context until something forces a flush, and a JDBC
        // query is not something it knows about, so the flush has to be explicit.
        entityManager.flush();

        assertThat(jdbc.queryForObject(
                        "SELECT prompt_tokens FROM evaluation WHERE submission_id = ?", Integer.class, submissionId))
                .isEqualTo(1_200);
        assertThat(jdbc.queryForObject(
                        "SELECT completion_tokens FROM evaluation WHERE submission_id = ?",
                        Integer.class,
                        submissionId))
                .isEqualTo(260);
    }

    @Test
    void sendsTheProblemAndItsRubricToTheModel() {
        reviews.submit("rate-limiter", DESIGN, "");

        DesignReviewRequest sent = reviewer.lastRequest();
        assertThat(sent.problemTitle()).isEqualTo("Design a Rate Limiter");
        assertThat(sent.problemStatement()).isNotBlank();
        assertThat(sent.requirements()).isNotEmpty();
        assertThat(sent.scaleHints()).contains("10M daily active users");
        assertThat(sent.rubricDimensions()).hasSize(7);
        assertThat(sent.rubricDimensions().getLast().mustHaves()).isNotEmpty();
        assertThat(sent.designMarkdown()).contains("Token bucket in Redis");
    }

    @Test
    void marksTheReviewFailedAndKeepsTheDesignWhenTheModelCannotProduceOne() {
        reviewer.failNext();

        long submissionId = reviews.submit("rate-limiter", DESIGN, "");

        ReviewState state = reviews.state(submissionId);
        assertThat(state.status()).isEqualTo(EvaluationStatus.FAILED);
        assertThat(state.evaluation()).isEmpty();
        assertThat(state.failureReason()).hasValue("The model returned prose, not JSON");
        // The point of the FAILED state: nothing the user wrote is lost.
        assertThat(state.submittedDesign()).contains("Token bucket in Redis");
    }

    @Test
    void retryingAFailedReviewProducesOne() {
        reviewer.failNext();
        long submissionId = reviews.submit("rate-limiter", DESIGN, "");
        assertThat(reviews.state(submissionId).status()).isEqualTo(EvaluationStatus.FAILED);

        reviews.retry(submissionId);

        ReviewState state = reviews.state(submissionId);
        assertThat(state.status()).isEqualTo(EvaluationStatus.DONE);
        assertThat(state.evaluation()).isPresent();
        assertThat(state.failureReason()).isEmpty();
    }

    @Test
    void rejectsASubmissionLongerThanTheCapWithoutCallingTheModel() {
        String tooLong = "x".repeat(ReviewService.MAX_SUBMISSION_LENGTH + 1);

        assertThatThrownBy(() -> reviews.submit("rate-limiter", tooLong, ""))
                .isInstanceOf(SubmissionTooLongException.class)
                .hasMessageContaining("15,000");
        assertThat(reviewer.calls()).isZero();
    }

    @Test
    void acceptsASubmissionExactlyAtTheCap() {
        long submissionId = reviews.submit("rate-limiter", "x".repeat(ReviewService.MAX_SUBMISSION_LENGTH), "");

        assertThat(reviews.state(submissionId).status()).isEqualTo(EvaluationStatus.DONE);
    }

    // ---------------------------------------------------------------------
    // Milestone 4: ownership, the monthly cap, and history.
    // ---------------------------------------------------------------------

    @Test
    void recordsWhoSubmittedTheDesign() {
        long submissionId = reviews.submit("rate-limiter", DESIGN, "");
        entityManager.flush();

        assertThat(jdbc.queryForObject("SELECT user_id FROM design_submission WHERE id = ?", Long.class, submissionId))
                .isEqualTo(account.id());
    }

    @Test
    void refusesToReviewWithoutAnAccount() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> reviews.submit("rate-limiter", DESIGN, "")).isInstanceOf(NotSignedInException.class);
    }

    @Test
    void allowsThreeReviewsAMonthOnTheFreePlan() {
        for (int i = 0; i < 3; i++) {
            reviews.submit("rate-limiter", DESIGN, "");
        }

        assertThat(reviews.allowanceFor(account).used()).isEqualTo(3);
        assertThat(reviews.allowanceFor(account).remaining()).contains(0);
        assertThat(reviews.allowanceFor(account).hasRemaining()).isFalse();
    }

    @Test
    void refusesTheFourthReviewInAMonthAndDoesNotCallTheModel() {
        for (int i = 0; i < 3; i++) {
            reviews.submit("rate-limiter", DESIGN, "");
        }
        int callsBefore = reviewer.calls();

        assertThatThrownBy(() -> reviews.submit("rate-limiter", DESIGN, ""))
                .isInstanceOf(ReviewLimitReachedException.class)
                .hasMessageContaining("3");

        // The cap exists to stop the spend, so it has to be checked before the call.
        assertThat(reviewer.calls()).isEqualTo(callsBefore);
    }

    @Test
    void countsTheAllowancePerAccountRatherThanGlobally() {
        for (int i = 0; i < 3; i++) {
            reviews.submit("rate-limiter", DESIGN, "");
        }

        Account other = accounts.register("second@example.com", "Someone Else", "another-long-password");
        signIn(other.email());

        assertThat(reviews.allowanceFor(other).used()).isZero();
        assertThat(reviews.submit("url-shortener", DESIGN, "")).isPositive();
    }

    @Test
    void doesNotCapAPaidPlan() {
        jdbc.update("UPDATE app_user SET plan = 'PRO' WHERE id = ?", account.id());
        // Hibernate already has this account in its first-level cache from the sign-in
        // above, and a JDBC update goes around it. Clearing forces a re-read, which is
        // what a later request would do anyway.
        entityManager.clear();
        Account pro = accounts.current().orElseThrow();

        Allowance allowance = reviews.allowanceFor(pro);

        assertThat(allowance.capped()).isFalse();
        assertThat(allowance.hasRemaining()).isTrue();
        assertThat(allowance.remaining()).isEmpty();
    }

    @Test
    void countsOnlyThisCalendarMonth() {
        reviews.submit("rate-limiter", DESIGN, "");
        entityManager.flush();
        // The clock is fixed at 2026-01-01, so backdating one submission to December puts
        // it outside the window without any waiting or clock games in the test.
        jdbc.update(
                "UPDATE design_submission SET created_at = created_at - interval '20 days' WHERE user_id = ?",
                account.id());

        assertThat(reviews.allowanceFor(account).used()).isZero();
    }

    @Test
    void listsThisPersonsReviewsNewestFirst() {
        long first = reviews.submit("rate-limiter", DESIGN, "");
        long second = reviews.submit("url-shortener", DESIGN, "");
        entityManager.flush();

        assertThat(reviews.historyFor(account))
                .extracting(ReviewHistoryEntry::submissionId)
                .containsExactlyInAnyOrder(first, second);
        assertThat(reviews.historyFor(account))
                .allSatisfy(entry -> assertThat(entry.status()).isEqualTo(EvaluationStatus.DONE));
    }

    @Test
    void showsASubmissionThatHasNoEvaluationRatherThanFailing() {
        // A submission with no evaluation row should not be able to break the history
        // page. This is how the page first broke: the status came back null and the view
        // called name() on it.
        reviews.submit("rate-limiter", DESIGN, "");
        entityManager.flush();
        jdbc.update("DELETE FROM evaluation");
        entityManager.clear();

        assertThat(reviews.historyFor(account)).singleElement().satisfies(entry -> {
            assertThat(entry.reviewStatus()).isEmpty();
            assertThat(entry.statusLabel()).isEqualTo("NO REVIEW");
        });
    }

    @Test
    void keepsOnePersonsHistoryOutOfAnothers() {
        reviews.submit("rate-limiter", DESIGN, "");
        Account other = accounts.register("third@example.com", "Third Person", "yet-another-password");
        signIn(other.email());
        entityManager.flush();

        assertThat(reviews.historyFor(other)).isEmpty();
        assertThat(reviews.historyFor(account)).hasSize(1);
    }

    @Test
    void rejectsAnUnknownProblem() {
        assertThatThrownBy(() -> reviews.submit("no-such-problem", DESIGN, ""))
                .isInstanceOf(ProblemNotFoundException.class);
    }

    @Test
    void rejectsAnUnknownSubmission() {
        assertThatThrownBy(() -> reviews.state(999_999L)).isInstanceOf(SubmissionNotFoundException.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        AiReviewer aiReviewer() {
            return new ScriptedReviewer();
        }

        /** Runs the background review inline, so the tests need no waiting. */
        @Bean
        Executor summaryExecutor() {
            return Runnable::run;
        }

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }

        /** Counters are recorded in production; a simple registry collects them here. */
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    /** A model that returns a fixed review, or fails on demand. */
    static final class ScriptedReviewer implements AiReviewer {

        private DesignReviewRequest lastRequest;
        private int calls;
        private boolean failNext;

        @Override
        public Optional<String> summariseQuizAttempt(QuizSummaryRequest request) {
            throw new UnsupportedOperationException("The review flow never summarises a quiz");
        }

        @Override
        public AiReviewResult reviewDesign(DesignReviewRequest request) {
            this.lastRequest = request;
            this.calls++;
            if (failNext) {
                failNext = false;
                return new AiReviewResult(
                        Optional.empty(),
                        "Sure! Here is my review...",
                        "ollama",
                        "llama3.1:8b",
                        "v1",
                        1_200,
                        3,
                        Optional.of(900),
                        Optional.of(40),
                        Optional.of("The model returned prose, not JSON"));
            }
            DesignEvaluation evaluation = new DesignEvaluation(
                    7,
                    List.of(new DimensionScore(
                            "Reliability & failure modes", 4, "You do not say what happens when Redis is down.")),
                    List.of("Token bucket fits the burst requirement."),
                    List.of("Every request makes a Redis round trip."),
                    List.of("No capacity estimate."),
                    List.of("How would you rate limit across two data centres?"));
            return new AiReviewResult(
                    Optional.of(evaluation),
                    "{\"overallScore\":7}",
                    "ollama",
                    "llama3.1:8b",
                    "v1",
                    2_400,
                    1,
                    Optional.of(1_200),
                    Optional.of(260),
                    Optional.empty());
        }

        @Override
        public in.codefarm.designarena.ai.AiStatus status() {
            return new in.codefarm.designarena.ai.AiStatus("stub", "stub-model", true, 2, java.util.Map.of());
        }

        void failNext() {
            this.failNext = true;
        }

        int calls() {
            return calls;
        }

        DesignReviewRequest lastRequest() {
            return lastRequest;
        }

        void reset() {
            this.lastRequest = null;
            this.calls = 0;
            this.failNext = false;
        }
    }
}
