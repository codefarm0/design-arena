package in.codefarm.designarena.ai;

import java.util.List;

/**
 * One rubric dimension as the prompt describes it.
 *
 * @param name the dimension name the model must use in its reply
 * @param description what this dimension judges
 * @param mustHaves the specific points to look for
 */
public record RubricDimensionPrompt(String name, String description, List<String> mustHaves) {

    public RubricDimensionPrompt {
        mustHaves = List.copyOf(mustHaves);
    }
}
