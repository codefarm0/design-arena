package in.codefarm.designarena.quiz;

import java.util.Optional;

/**
 * A stored attempt, scored.
 *
 * @param attemptId the attempt
 * @param problemId the problem it is about
 * @param scored the score, categories and per-question results
 * @param summary the stored AI narrative, if one was produced
 * @param submitted whether the attempt has been submitted
 */
record AttemptState(
        long attemptId, long problemId, ScoredAttempt scored, Optional<String> summary, boolean submitted) {}
