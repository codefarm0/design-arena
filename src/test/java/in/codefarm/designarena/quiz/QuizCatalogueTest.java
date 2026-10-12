package in.codefarm.designarena.quiz;

import in.codefarm.designarena.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Question counts come back as one grouped query, one entry per problem that has any. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, QuizCatalogue.class})
class QuizCatalogueTest {

    private final QuizCatalogue catalogue;

    QuizCatalogueTest(QuizCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    @Test
    void countsTenSeededQuestionsForEveryProblem() {
        Map<Long, Long> counts = catalogue.questionCountsByProblemId();

        assertThat(counts).hasSize(3);
        assertThat(counts.values()).containsOnly(10L);
    }
}
