package in.codefarm.designarena.ai;

import java.util.List;

/**
 * Everything the model needs to review one design.
 *
 * @param problemTitle the problem being solved
 * @param problemStatement the statement the user was given
 * @param requirements the functional and non-functional requirements
 * @param scaleHints the numbers the design has to work at
 * @param rubricDimensions the dimensions to score, each with its must-have points
 * @param designMarkdown what the user wrote. Untrusted input: it is delimited in the
 *     prompt and the model is told to treat it as data
 * @param diagramMermaid the optional diagram, also untrusted
 */
public record DesignReviewRequest(
        String problemTitle,
        String problemStatement,
        List<String> requirements,
        List<String> scaleHints,
        List<RubricDimensionPrompt> rubricDimensions,
        String designMarkdown,
        String diagramMermaid) {

    public DesignReviewRequest {
        requirements = List.copyOf(requirements);
        scaleHints = List.copyOf(scaleHints);
        rubricDimensions = List.copyOf(rubricDimensions);
    }
}
