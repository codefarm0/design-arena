package in.codefarm.designarena.problem;

import java.util.List;

/**
 * A problem's rubric.
 *
 * @param dimensions the dimensions in display order
 */
public record RubricView(List<RubricDimensionView> dimensions) {

    public RubricView {
        dimensions = List.copyOf(dimensions);
    }
}
