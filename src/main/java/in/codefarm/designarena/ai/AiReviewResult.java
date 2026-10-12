package in.codefarm.designarena.ai;

import java.util.Optional;

/**
 * The outcome of one review call, including what has to be recorded about it.
 *
 * <p>The evaluation itself is optional; everything else is the evidence. Keeping the raw
 * response and the provider, model and prompt version is what makes it possible to
 * explain a strange review weeks later — by which time the prompt will have changed.
 *
 * @param evaluation the parsed review, absent when the model failed
 * @param rawResponse exactly what came back, kept even on success
 * @param provider which provider answered
 * @param model which model answered
 * @param promptVersion the version of the prompt that produced it
 * @param latencyMs how long the call took, including retries
 * @param attempts how many times the model was asked before giving up or succeeding
 * @param promptTokens tokens in the prompt, when the provider reports them
 * @param completionTokens tokens in the reply, when the provider reports them
 * @param failureReason why it failed, when it did
 */
public record AiReviewResult(
        Optional<DesignEvaluation> evaluation,
        String rawResponse,
        String provider,
        String model,
        String promptVersion,
        long latencyMs,
        int attempts,
        Optional<Integer> promptTokens,
        Optional<Integer> completionTokens,
        Optional<String> failureReason) {

    public boolean succeeded() {
        return evaluation.isPresent();
    }
}
