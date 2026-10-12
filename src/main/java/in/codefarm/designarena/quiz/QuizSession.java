package in.codefarm.designarena.quiz;

import java.util.List;

/**
 * An attempt in progress: the questions to ask and the attempt they are being answered
 * against.
 *
 * @param attemptId the attempt being answered
 * @param problemSlug the problem's URL identifier
 * @param problemTitle the problem's title, shown above the progress bar
 * @param questions every question, in order
 */
public record QuizSession(long attemptId, String problemSlug, String problemTitle, List<QuizQuestion> questions) {

    public QuizSession {
        questions = List.copyOf(questions);
    }
}
