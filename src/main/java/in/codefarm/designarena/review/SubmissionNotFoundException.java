package in.codefarm.designarena.review;

/** Thrown when a submission id does not exist, typically from a hand-edited URL. */
public class SubmissionNotFoundException extends RuntimeException {

    public SubmissionNotFoundException(long submissionId) {
        super("No design submission with id " + submissionId);
    }
}
