package in.codefarm.designarena.ai;

import java.util.List;

/**
 * What the model needs in order to describe the design a set of quiz answers implies.
 *
 * @param problemTitle the problem the quiz was about
 * @param score points earned
 * @param maxScore points available
 * @param decisions one line per question, saying what was chosen and whether it was right
 */
public record QuizSummaryRequest(String problemTitle, int score, int maxScore, List<String> decisions) {

    public QuizSummaryRequest {
        decisions = List.copyOf(decisions);
    }
}
