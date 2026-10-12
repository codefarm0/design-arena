package in.codefarm.designarena.problem;

import java.util.List;

/**
 * The editable shape of a problem, as the admin form submits it.
 *
 * <p>Requirements, scale hints and must-haves are ordered lists of plain strings, so the
 * form edits them as one line per item rather than as rows with their own add, remove and
 * reorder controls. That is a deliberate trade: it is the whole list on one screen, it
 * reorders by moving a line, and it costs no UI machinery. If these ever grow structure
 * beyond text, they earn a proper editor.
 *
 * @param slug the URL identifier; stable once published, because links depend on it
 * @param title the display title
 * @param difficulty EASY, MEDIUM or HARD
 * @param blurb the one line on the catalogue card
 * @param statementMarkdown the problem statement
 * @param functionalRequirements what the system must do, in order
 * @param nonFunctionalRequirements how well it must do it, in order
 * @param scaleHints the numbers to design against
 * @param active whether it appears in the catalogue
 */
public record ProblemDraft(
        String slug,
        String title,
        Difficulty difficulty,
        String blurb,
        String statementMarkdown,
        List<String> functionalRequirements,
        List<String> nonFunctionalRequirements,
        List<String> scaleHints,
        boolean active) {

    public ProblemDraft {
        functionalRequirements = List.copyOf(functionalRequirements);
        nonFunctionalRequirements = List.copyOf(nonFunctionalRequirements);
        scaleHints = List.copyOf(scaleHints);
    }
}
