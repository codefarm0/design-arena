package in.codefarm.designarena.problem;

import java.util.List;

/**
 * Everything the design editor shows about a problem.
 *
 * @param id the problem's identity
 * @param slug the URL identifier
 * @param title the display title
 * @param difficulty the difficulty chip
 * @param statementMarkdown the problem statement
 * @param functionalRequirements what the system must do, in order
 * @param nonFunctionalRequirements how well it must do it, in order
 * @param scaleHints the numbers to design against
 */
public record ProblemDetail(
        long id,
        String slug,
        String title,
        Difficulty difficulty,
        String statementMarkdown,
        List<String> functionalRequirements,
        List<String> nonFunctionalRequirements,
        List<String> scaleHints) {

    public ProblemDetail {
        functionalRequirements = List.copyOf(functionalRequirements);
        nonFunctionalRequirements = List.copyOf(nonFunctionalRequirements);
        scaleHints = List.copyOf(scaleHints);
    }
}
