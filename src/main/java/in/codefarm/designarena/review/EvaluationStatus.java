package in.codefarm.designarena.review;

/** Where a review has got to. These are the three states the results page is drawn for. */
public enum EvaluationStatus {
    /** Submitted, and the model has not finished answering. */
    PENDING,
    /** Reviewed. */
    DONE,
    /** The model could not produce a usable review. The submission is kept and can be retried. */
    FAILED
}
