package in.codefarm.designarena.quiz;

import java.util.List;

/**
 * How one question went, with everything the results page shows for it.
 *
 * @param position the question number
 * @param category the rubric category
 * @param text the question
 * @param answered whether the user selected anything at all
 * @param correct whether the selection earned the point
 * @param yourAnswers the option text the user selected, in display order
 * @param correctAnswers the option text that was correct
 * @param explanation why, written for someone who got it wrong
 */
public record QuestionResult(
        int position,
        String category,
        String text,
        boolean answered,
        boolean correct,
        List<String> yourAnswers,
        List<String> correctAnswers,
        String explanation) {

    public QuestionResult {
        yourAnswers = List.copyOf(yourAnswers);
        correctAnswers = List.copyOf(correctAnswers);
    }
}
