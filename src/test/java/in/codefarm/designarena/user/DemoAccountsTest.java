package in.codefarm.designarena.user;

import in.codefarm.designarena.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The local demo accounts: who they are, that the passwords in the README actually work,
 * and that running twice is harmless.
 *
 * <p>Role and plan are read with plain SQL rather than through a lookup added to
 * {@code Accounts} for the purpose. Production API should not grow methods that exist
 * only to make a test convenient.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, Accounts.class, DemoAccounts.class, DemoAccountsTest.TestBeans.class})
class DemoAccountsTest {

    private final DemoAccounts demoAccounts;
    private final Accounts accounts;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    DemoAccountsTest(DemoAccounts demoAccounts, Accounts accounts, PasswordEncoder passwordEncoder, JdbcTemplate jdbc) {
        this.demoAccounts = demoAccounts;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Test
    void createsAnAdminAndAPlainLearner() {
        demoAccounts.run(null);

        assertThat(accounts.exists(DemoAccounts.ADMIN_EMAIL)).isTrue();
        assertThat(accounts.exists(DemoAccounts.LEARNER_EMAIL)).isTrue();
    }

    @Test
    void givesOnlyTheAdminAccountTheAdminRole() {
        demoAccounts.run(null);

        assertThat(column("role", DemoAccounts.ADMIN_EMAIL)).isEqualTo("ADMIN");
        assertThat(column("role", DemoAccounts.LEARNER_EMAIL)).isEqualTo("USER");
    }

    @Test
    void putsBothAccountsOnTheFreePlanSoTheLimitIsVisible() {
        demoAccounts.run(null);

        assertThat(column("plan", DemoAccounts.ADMIN_EMAIL)).isEqualTo("FREE");
        assertThat(column("plan", DemoAccounts.LEARNER_EMAIL)).isEqualTo("FREE");
    }

    @Test
    void theDocumentedPasswordsAreTheOnesThatWork() {
        demoAccounts.run(null);

        // If somebody edits a password here without editing the README, this fails.
        assertThat(passwordEncoder.matches(
                        DemoAccounts.ADMIN_PASSWORD, column("password_hash", DemoAccounts.ADMIN_EMAIL)))
                .isTrue();
        assertThat(passwordEncoder.matches(
                        DemoAccounts.LEARNER_PASSWORD, column("password_hash", DemoAccounts.LEARNER_EMAIL)))
                .isTrue();
    }

    @Test
    void runningAgainChangesNothing() {
        demoAccounts.run(null);
        String originalHash = column("password_hash", DemoAccounts.ADMIN_EMAIL);

        demoAccounts.run(null);

        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM app_user WHERE email = ?", Long.class, DemoAccounts.ADMIN_EMAIL))
                .isEqualTo(1L);
        // A restart must not reset a password somebody changed while testing.
        assertThat(column("password_hash", DemoAccounts.ADMIN_EMAIL)).isEqualTo(originalHash);
    }

    private String column(String name, String email) {
        return jdbc.queryForObject("SELECT " + name + " FROM app_user WHERE email = ?", String.class, email);
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
