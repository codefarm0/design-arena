package in.codefarm.designarena.problem;

import in.codefarm.designarena.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The catalogue reads the seeded problems.
 *
 * <p>Runs against a real Postgres with the real migrations applied, so it covers the seed
 * data and the projection query together: if a migration stops producing three active
 * problems in order, this fails.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, ProblemCatalogue.class})
class ProblemCatalogueTest {

    private final ProblemCatalogue catalogue;

    ProblemCatalogueTest(ProblemCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    @Test
    void returnsSeededProblemsInCatalogueOrder() {
        List<ProblemSummary> problems = catalogue.activeProblems();

        assertThat(problems)
                .extracting(ProblemSummary::slug)
                .containsExactly("rate-limiter", "url-shortener", "notification-system");
    }

    @Test
    void exposesTheFieldsTheCatalogueCardNeeds() {
        ProblemSummary rateLimiter = catalogue.activeProblems().getFirst();

        assertThat(rateLimiter.id()).isPositive();
        assertThat(rateLimiter.title()).isEqualTo("Design a Rate Limiter");
        assertThat(rateLimiter.difficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(rateLimiter.blurb()).isNotBlank();
    }
}
