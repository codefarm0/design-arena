package in.codefarm.designarena.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Two accounts to sign in with on a fresh local database.
 *
 * <p><b>Local profile only, and that restriction is the whole design.</b> Seeding accounts
 * with published passwords through a Flyway migration would have created them in every
 * environment the migration ever runs in, including production — an administrator with a
 * password printed in a public README. A profile-scoped runner cannot do that: the
 * {@code cloud} profile never constructs this bean.
 *
 * <p>It is also idempotent, so restarting does not fail and does not reset a password
 * somebody changed while testing.
 */
@Component
@Profile("local")
class DemoAccounts implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAccounts.class);

    static final String ADMIN_EMAIL = "admin@designarena.local";
    static final String ADMIN_PASSWORD = "admin-local-demo";
    static final String LEARNER_EMAIL = "learner@designarena.local";
    static final String LEARNER_PASSWORD = "learner-local-demo";

    private final Accounts accounts;

    DemoAccounts(Accounts accounts) {
        this.accounts = accounts;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing(ADMIN_EMAIL, "Demo Admin", ADMIN_PASSWORD, true);
        createIfMissing(LEARNER_EMAIL, "Demo Learner", LEARNER_PASSWORD, false);
    }

    private void createIfMissing(String email, String displayName, String password, boolean admin) {
        if (accounts.exists(email)) {
            return;
        }
        accounts.register(email, displayName, password);
        if (admin) {
            accounts.grantAdmin(email);
        }
        log.info(
                "Created local demo account {} ({}). Local profile only; see the README.",
                email,
                admin ? "admin" : "free plan");
    }
}
