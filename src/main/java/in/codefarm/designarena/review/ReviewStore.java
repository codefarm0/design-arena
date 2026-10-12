package in.codefarm.designarena.review;

import in.codefarm.designarena.ai.AiReviewResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Every database operation a review needs, each in its own short transaction.
 *
 * <p>Separate from {@link ReviewService} for the same reason as in the quiz feature: the
 * model call takes a minute and must not happen inside a transaction.
 */
@Service
class ReviewStore {

    private final DesignSubmissionRepository submissions;
    private final EvaluationRepository evaluations;
    private final Clock clock;

    ReviewStore(DesignSubmissionRepository submissions, EvaluationRepository evaluations, Clock clock) {
        this.submissions = submissions;
        this.evaluations = evaluations;
        this.clock = clock;
    }

    /** Stores the design and opens a PENDING evaluation for it. */
    @Transactional
    long save(long userId, long problemId, String designMarkdown, String diagramMermaid) {
        Instant now = Instant.now(clock);
        DesignSubmission submission =
                submissions.save(new DesignSubmission(userId, problemId, designMarkdown, diagramMermaid, now));
        evaluations.save(new Evaluation(submission, now));
        return submission.getId();
    }

    @Transactional(readOnly = true)
    Evaluation latestFor(long submissionId) {
        return evaluations
                .findLatestBySubmissionId(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
    }

    /** Records the outcome of a review attempt, successful or not. */
    @Transactional
    void record(long submissionId, AiReviewResult result, String resultJson) {
        Evaluation evaluation = latestOrThrow(submissionId);
        Instant now = Instant.now(clock);
        evaluation.recordUsage(
                result.promptTokens().orElse(null), result.completionTokens().orElse(null));
        if (result.succeeded()) {
            evaluation.succeeded(
                    resultJson,
                    result.rawResponse(),
                    result.provider(),
                    result.model(),
                    result.promptVersion(),
                    result.latencyMs(),
                    now);
        } else {
            evaluation.failed(
                    result.rawResponse(),
                    result.provider(),
                    result.model(),
                    result.promptVersion(),
                    result.latencyMs(),
                    result.failureReason().orElse("The model did not return a usable review."),
                    now);
        }
        evaluations.save(evaluation);
    }

    /** Puts a failed evaluation back into PENDING so it can be attempted again. */
    @Transactional
    void reopen(long submissionId) {
        Evaluation evaluation = latestOrThrow(submissionId);
        evaluation.reopened();
        evaluations.save(evaluation);
    }

    /** How many designs this person submitted inside a window. */
    @Transactional(readOnly = true)
    long countSubmissionsBetween(long userId, Instant from, Instant to) {
        return submissions.countByUserIdAndCreatedAtBetween(userId, from, to);
    }

    /** Everything this person submitted, newest first. */
    @Transactional(readOnly = true)
    List<ReviewHistoryEntry> historyFor(long userId) {
        return submissions.findHistoryFor(userId);
    }

    private Evaluation latestOrThrow(long submissionId) {
        return evaluations
                .findLatestBySubmissionId(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
    }
}
