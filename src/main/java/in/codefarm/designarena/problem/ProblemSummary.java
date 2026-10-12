package in.codefarm.designarena.problem;

/**
 * What the rest of the application knows about a problem in a list context.
 *
 * <p>A record, not the entity: it crosses the feature boundary, carries exactly the fields
 * the catalogue card renders, and is detached from any persistence session.
 *
 * @param id the problem's database identity, used to correlate with data owned by other
 *     features, such as the quiz question count
 * @param slug the stable URL identifier
 * @param title the display title
 * @param difficulty the difficulty chip
 * @param blurb the one-line description on the card
 */
public record ProblemSummary(long id, String slug, String title, Difficulty difficulty, String blurb) {}
