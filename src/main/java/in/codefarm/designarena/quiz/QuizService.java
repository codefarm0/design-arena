package in.codefarm.designarena.quiz;

import in.codefarm.designarena.ai.AiReviewer;
import in.codefarm.designarena.ai.QuizSummaryRequest;
import in.codefarm.designarena.problem.ProblemCatalogue;
import in.codefarm.designarena.problem.ProblemSummary;
import in.codefarm.designarena.user.Account;
import in.codefarm.designarena.user.Accounts;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * The quiz, as the rest of the application uses it: start, answer, submit, read results.
 *
 * <p>Notice there is no {@code @Transactional} on this class. Submitting involves a call
 * to a language model that can take a minute on a local model, and holding a database
 * transaction open across it would pin a connection for the duration. The transactional
 * work lives in {@link QuizAttemptStore}, so submitting is three short steps: score and
 * persist, call the model, store the narrative.
 */
@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final QuizAttemptStore store;
    private final ProblemCatalogue problems;
    private final AiReviewer reviewer;
    private final Executor summaryExecutor;
    private final Accounts accounts;
    private final MeterRegistry meters;

    QuizService(
            QuizAttemptStore store,
            ProblemCatalogue problems,
            AiReviewer reviewer,
            Executor summaryExecutor,
            Accounts accounts,
            MeterRegistry meters) {
        this.store = store;
        this.problems = problems;
        this.reviewer = reviewer;
        this.summaryExecutor = summaryExecutor;
        this.accounts = accounts;
        this.meters = meters;
    }

    /** Begins an attempt at the given problem. */
    public QuizSession start(String problemSlug) {
        ProblemSummary problem = problems.bySlug(problemSlug);
        // The quiz works signed out: an anonymous attempt simply has no owner and so
        // never appears in anybody's history. Requiring an account to answer ten
        // multiple-choice questions would cost more users than it gains.
        Long userId = accounts.current().map(Account::id).orElse(null);
        StartedAttempt started = store.startAttempt(userId, problem.id());
        return new QuizSession(started.attemptId(), problem.slug(), problem.title(), started.questions());
    }

    /**
     * Records the selection for one question. An empty selection means the question was
     * skipped, and is stored as such.
     */
    public void answer(long attemptId, long questionId, Set<Long> selectedOptionIds) {
        store.recordAnswer(attemptId, questionId, selectedOptionIds);
    }

    /**
     * Scores the attempt and returns the results immediately.
     *
     * <p>The narrative is <b>not</b> waited for. A local model takes tens of seconds to
     * answer, and making the user stare at a frozen page for that long to receive one
     * optional paragraph is the wrong trade: the score, the categories and every
     * explanation are ready now. So the model call is handed to a background thread and
     * the summary appears on the results page when, and if, it arrives.
     */
    public QuizResults submit(long attemptId) {
        AttemptState state = store.submit(attemptId);
        // Counted here rather than inferred from the database later: a counter at the
        // point of the event is cheap, and it survives the row being deleted.
        meters.counter("designarena.quiz.submitted").increment();
        ProblemSummary problem = problems.byId(state.problemId());

        if (state.summary().isEmpty()) {
            summaryExecutor.execute(() -> writeSummary(problem, state));
        }
        return toResults(state, problem, state.summary());
    }

    /**
     * Produces the narrative and stores it. Runs on a background thread, so nothing here
     * may throw into a caller: a failed summary is a missing paragraph, not a failed quiz.
     */
    private void writeSummary(ProblemSummary problem, AttemptState state) {
        try {
            narrativeFor(problem, state).ifPresent(text -> store.attachSummary(state.attemptId(), text));
        } catch (RuntimeException e) {
            log.warn("Could not store a summary for attempt {}: {}", state.attemptId(), e.getMessage());
        }
    }

    /** This person's finished quizzes, newest first. */
    public List<QuizHistoryEntry> historyFor(Account account) {
        return store.historyFor(account.id());
    }

    /** The results of an attempt that has already been submitted. */
    public QuizResults results(long attemptId) {
        AttemptState state = store.load(attemptId);
        return toResults(state, problems.byId(state.problemId()), state.summary());
    }

    private Optional<String> narrativeFor(ProblemSummary problem, AttemptState state) {
        ScoredAttempt scored = state.scored();
        List<String> decisions =
                scored.questions().stream().map(QuizService::describe).toList();
        Optional<String> narrative = reviewer.summariseQuizAttempt(
                new QuizSummaryRequest(problem.title(), scored.score(), scored.maxScore(), decisions));
        if (narrative.isEmpty()) {
            log.info("No AI summary for attempt {}; the results page will render without it.", state.attemptId());
        }
        return narrative;
    }

    /** One line per question for the prompt: what was asked, what they picked, whether it was right. */
    private static String describe(QuestionResult result) {
        String chosen = result.answered() ? String.join(" and ", result.yourAnswers()) : "did not answer";
        String verdict = result.correct()
                ? "correct"
                : "incorrect, the right answer was " + String.join(" and ", result.correctAnswers());
        return "- [%s] %s -> chose: %s (%s)".formatted(result.category(), result.text(), chosen, verdict);
    }

    private static QuizResults toResults(AttemptState state, ProblemSummary problem, Optional<String> summary) {
        ScoredAttempt scored = state.scored();
        return new QuizResults(
                state.attemptId(),
                problem.slug(),
                problem.title(),
                scored.score(),
                scored.maxScore(),
                scored.categories(),
                scored.questions(),
                summary);
    }
}
