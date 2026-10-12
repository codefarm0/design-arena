package in.codefarm.designarena.quiz;

/**
 * Thrown when a question would be unanswerable or unscoreable if saved.
 *
 * <p>These are content mistakes, not code mistakes, and an editor needs to be told about
 * them in words rather than discover them when somebody fails a quiz unfairly.
 */
public class InvalidQuestionException extends RuntimeException {

    public InvalidQuestionException(String message) {
        super(message);
    }
}
