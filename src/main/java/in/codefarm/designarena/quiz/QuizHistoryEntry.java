package in.codefarm.designarena.quiz;

import java.time.Instant;

/**
 * One finished quiz attempt in somebody's history.
 *
 * @param attemptId the attempt, which is also its results URL
 * @param problemId the problem it was about
 * @param submittedAt when it was submitted
 * @param score points earned
 * @param maxScore points available
 */
public record QuizHistoryEntry(long attemptId, long problemId, Instant submittedAt, int score, int maxScore) {}
