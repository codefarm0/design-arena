package in.codefarm.designarena.review;

import java.time.Instant;
import java.util.Optional;

/**
 * One design review in somebody's history.
 *
 * @param submissionId the submission, which is also its URL
 * @param problemId the problem it was about
 * @param submittedAt when it was submitted
 * @param status where the review got to. <b>Nullable</b>: the query reads the latest
 *     evaluation for the submission, and a submission can exist with none — a row
 *     inserted by hand, or a future path that stores a design before reviewing it. A
 *     history page must show such a row, not fall over on it.
 */
public record ReviewHistoryEntry(long submissionId, long problemId, Instant submittedAt, EvaluationStatus status) {

    /** The status, when there is one. */
    public Optional<EvaluationStatus> reviewStatus() {
        return Optional.ofNullable(status);
    }

    /** What to show in a list: the status, or that no review was ever run. */
    public String statusLabel() {
        return reviewStatus().map(Enum::name).orElse("NO REVIEW");
    }
}
