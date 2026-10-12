package in.codefarm.designarena.problem;

/** Thrown when a problem slug does not match an active problem. */
public class ProblemNotFoundException extends RuntimeException {

    public ProblemNotFoundException(String slug) {
        super("No active problem with slug " + slug);
    }
}
