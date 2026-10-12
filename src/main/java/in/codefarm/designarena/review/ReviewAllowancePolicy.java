package in.codefarm.designarena.review;

import in.codefarm.designarena.user.Account;
import in.codefarm.designarena.user.UserPlan;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * How much reviewing a plan has left this month.
 *
 * <p>Extracted from {@link ReviewService} when that class reached nine constructor
 * parameters and the build said so. The limit was worth listening to: the plan, the
 * calendar and the clock are one concern — what somebody is allowed — and they had no
 * business sitting beside prompts, executors and JSON mappers.
 */
@Service
class ReviewAllowancePolicy {

    private final ReviewStore store;
    private final ReviewLimits limits;
    private final Clock clock;

    ReviewAllowancePolicy(ReviewStore store, ReviewLimits limits, Clock clock) {
        this.store = store;
        this.limits = limits;
        this.clock = clock;
    }

    /**
     * What this account has left.
     *
     * <p>A calendar month in UTC, not a rolling thirty days. It is easier to explain
     * ("resets on the 1st"), easier to display, and somebody can predict it without doing
     * arithmetic.
     */
    Allowance forAccount(Account account) {
        if (account.plan() == UserPlan.PRO) {
            return Allowance.unlimited();
        }
        YearMonth month = YearMonth.now(clock);
        Instant from = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        long used = store.countSubmissionsBetween(account.id(), from, to);
        return Allowance.of(
                limits.freeReviewsPerMonth(), used, month.plusMonths(1).atDay(1));
    }
}
