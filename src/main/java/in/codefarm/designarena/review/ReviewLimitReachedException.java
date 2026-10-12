package in.codefarm.designarena.review;

/**
 * Thrown when a free account has used its design reviews for the month.
 *
 * <p>Carries the numbers so the screen can say "3 of 3 used, resets on 1 November"
 * instead of "limit reached", which tells somebody nothing about what to do next.
 */
public class ReviewLimitReachedException extends RuntimeException {

    private final int limit;

    public ReviewLimitReachedException(int limit) {
        super("Free plan allows %d design reviews per month".formatted(limit));
        this.limit = limit;
    }

    public int limit() {
        return limit;
    }
}
