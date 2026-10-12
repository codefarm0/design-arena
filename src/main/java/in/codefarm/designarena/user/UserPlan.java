package in.codefarm.designarena.user;

/**
 * What a person is entitled to.
 *
 * <p>The plan exists before payments do, on purpose: every limit in the application is
 * already expressed in terms of it, so turning a payment into an entitlement later is a
 * change of one column rather than a change of policy everywhere.
 */
public enum UserPlan {
    /** The default. Design reviews are capped per month. */
    FREE,
    /** Paid. No review cap. */
    PRO
}
