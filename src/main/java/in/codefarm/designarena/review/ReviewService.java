package in.codefarm.designarena.review;

import in.codefarm.designarena.ai.AiReviewResult;
import in.codefarm.designarena.ai.AiReviewer;
import in.codefarm.designarena.ai.DesignEvaluation;
import in.codefarm.designarena.ai.DesignReviewRequest;
import in.codefarm.designarena.ai.RubricDimensionPrompt;
import in.codefarm.designarena.problem.ProblemCatalogue;
import in.codefarm.designarena.problem.ProblemDetail;
import in.codefarm.designarena.problem.ProblemSummary;
import in.codefarm.designarena.problem.RubricView;
import in.codefarm.designarena.user.Account;
import in.codefarm.designarena.user.Accounts;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Submitting a design and getting it reviewed.
 *
 * <p>The review runs asynchronously because it has to: a local model takes a minute or
 * more, which is far past what a user will hold a page open for. Submitting therefore
 * stores the design, opens a PENDING evaluation and returns straight away; the review
 * page shows the waiting state and picks up the result when it lands.
 */
@Service
public class ReviewService {

    /**
     * The longest submission we will send to a model.
     *
     * <p>A cap is not a style preference: without one, a single paste can cost a very
     * large number of tokens, and on a local model it can take long enough to look like a
     * hang. It is checked before anything is stored or sent.
     */
    public static final int MAX_SUBMISSION_LENGTH = 15_000;

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewStore store;
    private final ProblemCatalogue problems;
    private final AiReviewer reviewer;
    private final Executor reviewExecutor;
    /** Jackson 3, the same mapper Spring AI uses to read the model's reply. */
    private final JsonMapper json;

    private final Accounts accounts;
    private final ReviewAllowancePolicy allowances;
    private final MeterRegistry meters;

    ReviewService(
            ReviewStore store,
            ProblemCatalogue problems,
            AiReviewer reviewer,
            Executor summaryExecutor,
            JsonMapper json,
            Accounts accounts,
            ReviewAllowancePolicy allowances,
            MeterRegistry meters) {
        this.store = store;
        this.problems = problems;
        this.reviewer = reviewer;
        this.reviewExecutor = summaryExecutor;
        this.json = json;
        this.accounts = accounts;
        this.allowances = allowances;
        this.meters = meters;
    }

    /**
     * Stores a design and starts reviewing it.
     *
     * <p>Both checks happen before anything is stored or sent: the length cap, and the
     * monthly allowance. Charging somebody's allowance for a request that was never going
     * to be reviewed would be the worst of both.
     *
     * @return the submission id, for the review page's URL
     * @throws SubmissionTooLongException if the design exceeds {@link #MAX_SUBMISSION_LENGTH}
     * @throws ReviewLimitReachedException if a free account has used its month's reviews
     */
    public long submit(String problemSlug, String designMarkdown, String diagramMermaid) {
        String design = designMarkdown == null ? "" : designMarkdown.strip();
        String diagram = diagramMermaid == null ? "" : diagramMermaid.strip();
        if (design.length() > MAX_SUBMISSION_LENGTH) {
            throw new SubmissionTooLongException(design.length(), MAX_SUBMISSION_LENGTH);
        }

        Account account = accounts.currentOrFail();
        Allowance allowance = allowanceFor(account);
        if (!allowance.hasRemaining()) {
            // Worth counting: a rising number here is a pricing signal, not an error.
            meters.counter("designarena.review.blocked").increment();
            throw new ReviewLimitReachedException(allowance.limit().orElse(0));
        }

        ProblemDetail problem = problems.detailBySlug(problemSlug);
        long submissionId = store.save(account.id(), problem.id(), design, diagram);
        reviewExecutor.execute(() -> review(submissionId, problem, design, diagram));
        return submissionId;
    }

    /** Reviews a submission that previously failed. */
    public void retry(long submissionId) {
        ReviewState state = state(submissionId);
        ProblemDetail problem = problems.detailBySlug(state.problemSlug());
        store.reopen(submissionId);
        reviewExecutor.execute(() -> review(submissionId, problem, state.submittedDesign(), state.submittedDiagram()));
    }

    /** The current state of a review, for the page to render. */
    public ReviewState state(long submissionId) {
        Evaluation evaluation = store.latestFor(submissionId);
        DesignSubmission submission = evaluation.getSubmission();
        ProblemSummary problem = problems.byId(submission.getProblemId());

        return new ReviewState(
                submissionId,
                problem.slug(),
                problem.title(),
                evaluation.getStatus(),
                evaluation.getResultJson().flatMap(this::readEvaluation),
                submission.getContentMarkdown(),
                submission.getDiagramMermaid(),
                evaluation.getFailureReason(),
                provenanceOf(evaluation));
    }

    /**
     * Runs the review on a background thread.
     *
     * <p>Nothing here may throw into the caller: this runs after the user's request has
     * finished. Every outcome, including a model that never produces valid output, ends
     * as a stored status the page can show.
     */
    private void review(long submissionId, ProblemDetail problem, String design, String diagram) {
        try {
            RubricView rubric = problems.rubricFor(problem.id());
            AiReviewResult result = reviewer.reviewDesign(requestFor(problem, rubric, design, diagram));
            store.record(
                    submissionId,
                    result,
                    result.evaluation().flatMap(this::writeEvaluation).orElse(null));
            meters.counter("designarena.review.completed", "status", result.succeeded() ? "DONE" : "FAILED")
                    .increment();
            if (!result.succeeded()) {
                log.info(
                        "Review of submission {} failed after {} attempts: {}",
                        submissionId,
                        result.attempts(),
                        result.failureReason().orElse("unknown"));
            }
        } catch (RuntimeException e) {
            log.error("Reviewing submission {} threw unexpectedly", submissionId, e);
            store.record(submissionId, failureResult(e), null);
        }
    }

    /** What this month's allowance looks like for an account. */
    public Allowance allowanceFor(Account account) {
        return allowances.forAccount(account);
    }

    /** This person's design reviews, newest first. */
    public List<ReviewHistoryEntry> historyFor(Account account) {
        return store.historyFor(account.id());
    }

    private static DesignReviewRequest requestFor(
            ProblemDetail problem, RubricView rubric, String design, String diagram) {
        List<String> requirements = new ArrayList<>(problem.functionalRequirements());
        requirements.addAll(problem.nonFunctionalRequirements());
        List<RubricDimensionPrompt> dimensions = rubric.dimensions().stream()
                .map(dimension ->
                        new RubricDimensionPrompt(dimension.name(), dimension.description(), dimension.mustHaves()))
                .toList();
        return new DesignReviewRequest(
                problem.title(),
                problem.statementMarkdown(),
                requirements,
                problem.scaleHints(),
                dimensions,
                design,
                diagram);
    }

    private static AiReviewResult failureResult(RuntimeException cause) {
        return new AiReviewResult(
                Optional.empty(),
                "",
                "unknown",
                "unknown",
                "unknown",
                0,
                0,
                Optional.empty(),
                Optional.empty(),
                Optional.of("Unexpected error: " + cause.getMessage()));
    }

    private Optional<String> writeEvaluation(DesignEvaluation evaluation) {
        try {
            return Optional.of(json.writeValueAsString(evaluation));
        } catch (JacksonException e) {
            log.warn("Could not serialise an evaluation: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<DesignEvaluation> readEvaluation(String storedJson) {
        try {
            return Optional.of(json.readValue(storedJson, DesignEvaluation.class));
        } catch (JacksonException e) {
            log.warn("Stored evaluation JSON could not be read: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<String> provenanceOf(Evaluation evaluation) {
        if (evaluation.getModel() == null) {
            return Optional.empty();
        }
        return Optional.of("Reviewed by %s via %s · prompt %s"
                .formatted(evaluation.getModel(), evaluation.getProvider(), evaluation.getPromptVersion()));
    }
}
