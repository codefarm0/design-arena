package in.codefarm.designarena.problem;

import java.util.List;

/**
 * One editable rubric dimension.
 *
 * @param name the dimension name, which the model is told to use verbatim in its reply
 * @param description what this dimension judges
 * @param weight its relative weight
 * @param mustHaves the specific points a staff-level answer should cover
 */
public record RubricDimensionDraft(String name, String description, int weight, List<String> mustHaves) {

    public RubricDimensionDraft {
        mustHaves = List.copyOf(mustHaves);
    }
}
