package in.codefarm.designarena.user;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Registering an account, and finding out who is signed in.
 *
 * <p>This is the only way the rest of the application learns about people. It reads the
 * current user from Spring Security's context rather than from anything Vaadin-specific,
 * so services stay testable without a UI and without a browser session.
 */
@Service
public class Accounts {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    Accounts(AppUserRepository users, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Creates an account on the free plan.
     *
     * @throws EmailAlreadyRegisteredException if that address already has an account
     */
    @Transactional
    public Account register(String email, String displayName, String rawPassword) {
        String normalised = normalise(email);
        if (users.findByEmailIgnoringCase(normalised).isPresent()) {
            throw new EmailAlreadyRegisteredException(normalised);
        }
        AppUser user =
                new AppUser(normalised, displayName.strip(), passwordEncoder.encode(rawPassword), Instant.now(clock));
        return users.save(user).toAccount();
    }

    /**
     * Grants the admin role to an existing account.
     *
     * <p>There is no screen for this, deliberately: a self-service route to the role that
     * can edit every rubric is a liability. Production promotes an account with one line
     * of SQL; the local demo seeder uses this.
     */
    @Transactional
    public void grantAdmin(String email) {
        AppUser user = users.findByEmailIgnoringCase(normalise(email))
                .orElseThrow(() -> new IllegalArgumentException("No account for " + email));
        user.grantAdmin();
    }

    /** Whether an account already exists for this address. */
    @Transactional(readOnly = true)
    public boolean exists(String email) {
        return users.findByEmailIgnoringCase(normalise(email)).isPresent();
    }

    /** The signed-in person, or empty when browsing anonymously. */
    @Transactional(readOnly = true)
    public Optional<Account> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // An anonymous visitor still has an Authentication, and it reports itself as
        // authenticated. Checking the token type is the reliable test; comparing the
        // principal against the string "anonymousUser" is the one everybody writes first,
        // and it is fragile even before you notice it was written with ==.
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return users.findByEmailIgnoringCase(authentication.getName()).map(AppUser::toAccount);
    }

    /**
     * The signed-in person, for code that only runs behind authentication.
     *
     * @throws NotSignedInException if nobody is signed in, which would mean a view was
     *     reachable anonymously that should not have been
     */
    public Account currentOrFail() {
        return current().orElseThrow(NotSignedInException::new);
    }

    /** Lower-cased and trimmed, so one person cannot hold two accounts differing in case. */
    private static String normalise(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
