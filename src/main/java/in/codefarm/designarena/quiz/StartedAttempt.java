package in.codefarm.designarena.quiz;

import java.util.List;

/**
 * A freshly created attempt and the questions it will ask.
 *
 * @param attemptId the new attempt
 * @param questions the questions, in display order
 */
record StartedAttempt(long attemptId, List<QuizQuestion> questions) {

    StartedAttempt {
        questions = List.copyOf(questions);
    }
}
