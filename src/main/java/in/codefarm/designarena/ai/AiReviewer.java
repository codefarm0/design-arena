package in.codefarm.designarena.ai;

import java.util.Optional;

/**
 * The one way this application talks to a language model.
 *
 * <p>Everything else in the codebase depends on this interface and on nothing from Spring
 * AI or any vendor SDK. That is what makes switching provider a configuration change: the
 * implementation behind this interface is the only code that knows a provider exists.
 *
 * <p>Every method returns something the caller can use when the model does not answer.
 * A model is a remote dependency with no availability guarantee, so "it failed" is a
 * normal outcome to be designed for, not an exception to be propagated to a user.
 */
public interface AiReviewer {

    /**
     * Describes, in a few sentences, the design implied by a set of quiz answers.
     *
     * @param request the problem, the score and what was chosen
     * @return the narrative, or empty if the model was unavailable, too slow, or disabled.
     *     Callers must render their page without it.
     */
    Optional<String> summariseQuizAttempt(QuizSummaryRequest request);

    /**
     * Reviews a design against a rubric, as structured data.
     *
     * <p>Unlike the quiz summary, this result is the feature rather than a garnish, so the
     * outcome carries the evidence as well: the raw response, which provider and model
     * answered, the prompt version and the latency. The caller persists all of it.
     *
     * @param request the problem, the rubric and the user's submission
     * @return the outcome, which reports failure rather than throwing. A caller must
     *     handle {@code evaluation().isEmpty()} by recording a failed evaluation the user
     *     can retry.
     */
    AiReviewResult reviewDesign(DesignReviewRequest request);

    /**
     * What this layer is configured to do.
     *
     * <p>Part of the interface rather than a separate bean so that the one object that
     * knows which provider is wired in is also the one that reports it.
     */
    AiStatus status();
}
