package in.codefarm.designarena.quiz;

/**
 * The score within one category, such as Scaling or Failure handling.
 *
 * @param category the category name
 * @param score points earned
 * @param maxScore points available
 */
public record CategoryScore(String category, int score, int maxScore) {

    /** Percentage earned, for the width of the bar on the results page. */
    public int percentage() {
        return maxScore == 0 ? 0 : Math.round(score * 100f / maxScore);
    }
}
