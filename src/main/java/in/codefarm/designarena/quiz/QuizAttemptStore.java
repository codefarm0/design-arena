package in.codefarm.designarena.quiz;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Every database operation an attempt needs, each in its own short transaction.
 *
 * <p>This is a separate object from {@link QuizService} for one specific reason: submitting
 * an attempt involves a model call that can take a minute, and a database transaction must
 * not be held open across it. Keeping the transactional work here lets the service do
 * "write, call the model, write again" as three short steps instead of one long one.
 */
@Service
class QuizAttemptStore {

    private final QuestionRepository questions;
    private final QuizAttemptRepository attempts;
    private final Clock clock;

    QuizAttemptStore(QuestionRepository questions, QuizAttemptRepository attempts, Clock clock) {
        this.questions = questions;
        this.attempts = attempts;
        this.clock = clock;
    }

    /** Creates an attempt and returns it together with the questions to ask. */
    @Transactional
    StartedAttempt startAttempt(Long userId, long problemId) {
        QuizAttempt attempt = attempts.save(new QuizAttempt(userId, problemId, Instant.now(clock)));
        List<QuizQuestion> toAsk = questions.findByProblemIdWithOptions(problemId).stream()
                .map(QuizMapping::toQuizQuestion)
                .toList();
        return new StartedAttempt(attempt.getId(), toAsk);
    }

    /**
     * Stores what the user selected for one question, replacing any earlier selection.
     *
     * <p>Answers are saved as the user moves through the quiz rather than all at once at
     * the end, so closing the tab on question seven does not throw away the first six.
     */
    @Transactional
    void recordAnswer(long attemptId, long questionId, Set<Long> optionIds) {
        QuizAttempt attempt =
                attempts.findByIdWithAnswers(attemptId).orElseThrow(() -> new QuizNotFoundException(attemptId));
        if (attempt.isSubmitted()) {
            throw new QuizAlreadySubmittedException(attemptId);
        }
        attempt.getAnswers().stream()
                .filter(answer -> answer.getQuestionId().equals(questionId))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.replaceSelection(optionIds),
                        () -> attempt.addAnswer(new QuizAnswer(attempt, questionId, optionIds)));
        attempts.save(attempt);
    }

    /** Scores the attempt, closes it, and returns the result. Submitting twice is harmless. */
    @Transactional
    AttemptState submit(long attemptId) {
        QuizAttempt attempt =
                attempts.findByIdWithAnswers(attemptId).orElseThrow(() -> new QuizNotFoundException(attemptId));
        ScoredAttempt scored = score(attempt);
        if (!attempt.isSubmitted()) {
            attempt.submit(Instant.now(clock), scored.score(), scored.maxScore());
            attempts.save(attempt);
        }
        return new AttemptState(attemptId, attempt.getProblemId(), scored, attempt.getSummaryText(), true);
    }

    /** This person's submitted attempts, newest first. */
    @Transactional(readOnly = true)
    List<QuizHistoryEntry> historyFor(long userId) {
        return attempts.findHistoryFor(userId);
    }

    /** Re-reads and re-scores a stored attempt, for the results page. */
    @Transactional(readOnly = true)
    AttemptState load(long attemptId) {
        QuizAttempt attempt =
                attempts.findByIdWithAnswers(attemptId).orElseThrow(() -> new QuizNotFoundException(attemptId));
        return new AttemptState(
                attemptId, attempt.getProblemId(), score(attempt), attempt.getSummaryText(), attempt.isSubmitted());
    }

    /** Stores the narrative once the model has answered. */
    @Transactional
    void attachSummary(long attemptId, String summary) {
        QuizAttempt attempt = attempts.findById(attemptId).orElseThrow(() -> new QuizNotFoundException(attemptId));
        attempt.attachSummary(summary);
        attempts.save(attempt);
    }

    private ScoredAttempt score(QuizAttempt attempt) {
        List<ScorableQuestion> scorable = questions.findByProblemIdWithOptions(attempt.getProblemId()).stream()
                .map(QuizMapping::toScorable)
                .toList();
        Map<Long, Set<Long>> selections = attempt.getAnswers().stream()
                .collect(Collectors.toMap(
                        QuizAnswer::getQuestionId, QuizAnswer::getSelectedOptionIds, (a, b) -> a, LinkedHashMap::new));
        return QuizScorer.score(scorable, selections);
    }
}
