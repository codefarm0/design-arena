package in.codefarm.designarena.user;

/**
 * A signed-in person, as the rest of the application sees them.
 *
 * <p>Note what is absent: the password hash. It never leaves this package.
 *
 * @param id the account's identity, used to own attempts and submissions
 * @param email the login name, stored lower-cased
 * @param displayName what to call them on screen
 * @param plan what they are entitled to
 * @param role what they are trusted with
 */
public record Account(long id, String email, String displayName, UserPlan plan, UserRole role) {

    /** Whether this person may reach the admin area. */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
