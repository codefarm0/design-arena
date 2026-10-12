package in.codefarm.designarena.review;

import java.time.LocalDate;
import java.util.Optional;

/**
 * How much reviewing somebody has left this month.
 *
 * <p>Carries the numbers rather than a bare yes or no, so a screen can say "2 of 3 left,
 * resets on 1 November" — which tells somebody what to do next, as "limit reached" does
 * not.
 *
 * @param limit reviews allowed this month, or empty when the plan is not capped
 * @param used reviews already submitted this month
 * @param resetsOn the first day of next month, when a capped allowance refills
 */
public record Allowance(Optional<Integer> limit, long used, Optional<LocalDate> resetsOn) {

    static Allowance unlimited() {
        return new Allowance(Optional.empty(), 0, Optional.empty());
    }

    static Allowance of(int limit, long used, LocalDate resetsOn) {
        return new Allowance(Optional.of(limit), used, Optional.of(resetsOn));
    }

    /** Whether another review may be started. */
    public boolean hasRemaining() {
        return limit.map(allowed -> used < allowed).orElse(true);
    }

    /** Reviews left, when the plan is capped. */
    public Optional<Integer> remaining() {
        return limit.map(allowed -> Math.max(0, allowed - (int) used));
    }

    /** Whether this plan is capped at all. */
    public boolean capped() {
        return limit.isPresent();
    }
}
