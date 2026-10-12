package in.codefarm.designarena.problem;

import java.util.List;

/**
 * The editable shape of a rubric.
 *
 * @param dimensions the dimensions, in the order they are scored and displayed
 */
public record RubricDraft(List<RubricDimensionDraft> dimensions) {

    public RubricDraft {
        dimensions = List.copyOf(dimensions);
    }
}
