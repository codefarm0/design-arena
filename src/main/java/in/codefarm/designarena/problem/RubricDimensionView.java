package in.codefarm.designarena.problem;

import java.util.List;

/**
 * One rubric dimension, as the review prompt and the results page need it.
 *
 * @param name the dimension name, shown beside its score
 * @param description what it judges, sent to the model
 * @param weight its relative weight in the overall score
 * @param mustHaves the specific points a staff-level answer should cover
 */
public record RubricDimensionView(String name, String description, int weight, List<String> mustHaves) {

    public RubricDimensionView {
        mustHaves = List.copyOf(mustHaves);
    }
}
