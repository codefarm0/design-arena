package in.codefarm.designarena.user;

/**
 * What a person is trusted with.
 *
 * <p>Separate from {@link UserPlan} on purpose: a plan is what somebody paid for, a role
 * is what they are allowed to break. Conflating the two would make a paid subscription
 * an administrator.
 */
public enum UserRole {
    USER,
    ADMIN
}
