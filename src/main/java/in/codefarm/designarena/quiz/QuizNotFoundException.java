package in.codefarm.designarena.quiz;

/** Thrown when an attempt id does not exist, typically because a URL was edited by hand. */
public class QuizNotFoundException extends RuntimeException {

    public QuizNotFoundException(long attemptId) {
        super("No quiz attempt with id " + attemptId);
    }
}
