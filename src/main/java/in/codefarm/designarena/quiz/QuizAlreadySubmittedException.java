package in.codefarm.designarena.quiz;

/** Thrown when an answer is recorded against an attempt that has already been submitted. */
public class QuizAlreadySubmittedException extends RuntimeException {

    public QuizAlreadySubmittedException(long attemptId) {
        super("Quiz attempt " + attemptId + " has already been submitted");
    }
}
