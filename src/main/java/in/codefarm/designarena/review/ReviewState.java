package in.codefarm.designarena.review;

import in.codefarm.designarena.ai.DesignEvaluation;

import java.util.Optional;

/**
 * Everything the review page renders, in whichever state the review is in.
 *
 * @param submissionId the submission being reviewed
 * @param problemSlug the problem's URL identifier
 * @param problemTitle the problem's title
 * @param status PENDING, DONE or FAILED — the three states the page is drawn for
 * @param evaluation the review itself, present only when DONE
 * @param submittedDesign what the user wrote, shown beside the review
 * @param submittedDiagram the diagram they drew, if any
 * @param failureReason why it failed, present only when FAILED
 * @param provenance which model produced it, shown in the footer
 */
public record ReviewState(
        long submissionId,
        String problemSlug,
        String problemTitle,
        EvaluationStatus status,
        Optional<DesignEvaluation> evaluation,
        String submittedDesign,
        String submittedDiagram,
        Optional<String> failureReason,
        Optional<String> provenance) {}
