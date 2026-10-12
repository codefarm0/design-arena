package in.codefarm.designarena.review;

/**
 * Thrown when a submission is longer than the model will usefully read.
 *
 * <p>Checked before the model is called, not after: validation that prevents an expensive
 * call belongs at the boundary.
 */
public class SubmissionTooLongException extends RuntimeException {

    public SubmissionTooLongException(int length, int limit) {
        super("Submission is %,d characters; the limit is %,d".formatted(length, limit));
    }
}
