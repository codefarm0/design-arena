package in.codefarm.designarena.problem;

/**
 * How hard a problem is, shown as a chip on the catalogue card.
 *
 * <p>Stored as a string in the database rather than an ordinal, so inserting a new level
 * later cannot silently change the meaning of existing rows.
 */
public enum Difficulty {
    EASY,
    MEDIUM,
    HARD
}
