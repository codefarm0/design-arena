package in.codefarm.designarena.user;

import in.codefarm.designarena.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Registering, signing in, and the rules about who is who. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, Accounts.class, AppUserDetailsService.class, AccountsTest.TestBeans.class})
class AccountsTest {

    private final Accounts accounts;
    private final AppUserDetailsService userDetails;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    AccountsTest(
            Accounts accounts, AppUserDetailsService userDetails, PasswordEncoder passwordEncoder, JdbcTemplate jdbc) {
        this.accounts = accounts;
        this.userDetails = userDetails;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registersOnTheFreePlan() {
        Account account = accounts.register("alice@example.com", "Alice", "a-long-enough-password");

        assertThat(account.id()).isPositive();
        assertThat(account.plan()).isEqualTo(UserPlan.FREE);
        assertThat(account.displayName()).isEqualTo("Alice");
    }

    @Test
    void neverStoresThePasswordItself() {
        accounts.register("bob@example.com", "Bob", "correct-horse-battery");

        String stored = jdbc.queryForObject(
                "SELECT password_hash FROM app_user WHERE email = ?", String.class, "bob@example.com");
        assertThat(stored).isNotNull().doesNotContain("correct-horse-battery").startsWith("$2");
        assertThat(passwordEncoder.matches("correct-horse-battery", stored)).isTrue();
    }

    @Test
    void treatsEmailAsCaseInsensitive() {
        accounts.register("Carol@Example.com", "Carol", "a-long-enough-password");

        assertThatThrownBy(() -> accounts.register("carol@example.com", "Impostor", "another-long-password"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        assertThat(userDetails.loadUserByUsername("CAROL@EXAMPLE.COM")).isNotNull();
    }

    @Test
    void storesTheEmailLowerCased() {
        Account account = accounts.register("  Dave@Example.COM ", "Dave", "a-long-enough-password");

        assertThat(account.email()).isEqualTo("dave@example.com");
    }

    @Test
    void doesNotRevealWhetherAnEmailIsRegistered() {
        assertThatThrownBy(() -> userDetails.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                // Saying "no account for that address" would let anybody enumerate users.
                .hasMessageNotContainingAny("nobody@example.com", "not found", "no account");
    }

    @Test
    void grantsEveryoneTheSameSingleRole() {
        accounts.register("erin@example.com", "Erin", "a-long-enough-password");

        UserDetails details = userDetails.loadUserByUsername("erin@example.com");

        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
    }

    @Test
    void reportsNobodyWhenBrowsingAnonymously() {
        assertThat(accounts.current()).isEmpty();

        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken(
                        "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        // An anonymous visitor carries an Authentication that reports itself as
        // authenticated; only the token type distinguishes it.
        assertThat(accounts.current()).isEmpty();
    }

    @Test
    void resolvesTheSignedInAccountFromTheSecurityContext() {
        Account registered = accounts.register("frank@example.com", "Frank", "a-long-enough-password");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("frank@example.com", "n/a", List.of()));

        assertThat(accounts.current()).contains(registered);
        assertThat(accounts.currentOrFail().displayName()).isEqualTo("Frank");
    }

    @Test
    void failsLoudlyWhenCodeThatNeedsAnAccountRunsWithout() {
        assertThatThrownBy(accounts::currentOrFail).isInstanceOf(NotSignedInException.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
