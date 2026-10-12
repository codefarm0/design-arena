package in.codefarm.designarena.ai;

import in.codefarm.designarena.TestcontainersConfiguration;
import in.codefarm.designarena.problem.ProblemCatalogue;
import in.codefarm.designarena.problem.ProblemDetail;
import in.codefarm.designarena.problem.RubricView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Does the reviewer actually tell a good design from a bad one?
 *
 * <p>This is an <b>eval</b>, not a unit test. It calls the real configured model, so it
 * is slow, costs money on a paid provider, and is not deterministic. It is skipped unless
 * you ask for it:
 *
 * <pre>./gradlew test -Devals=true --tests '*DesignReviewEvalTest*'</pre>
 *
 * <p>The database is a Testcontainer so the seeded problems and rubrics are real; the
 * model is whatever the active profile points at, which locally is Ollama.
 *
 * <p>The assertion is deliberately coarse. The exact score a model gives will drift
 * between models and prompt versions; what must not drift is the ordering. A prompt edit
 * that makes a weak design score as well as a strong one is a regression, and no unit
 * test with a stubbed model could ever catch it.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@EnabledIfSystemProperty(named = "evals", matches = "true")
class DesignReviewEvalTest {

    private static final Logger log = LoggerFactory.getLogger(DesignReviewEvalTest.class);

    /** How much better a strong answer must score than a weak one to count as discriminating. */
    private static final int REQUIRED_MARGIN = 2;

    private final AiReviewer reviewer;
    private final ProblemCatalogue problems;

    DesignReviewEvalTest(AiReviewer reviewer, ProblemCatalogue problems) {
        this.reviewer = reviewer;
        this.problems = problems;
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"rate-limiter", "url-shortener", "notification-system"})
    @DisplayName("scores a strong design above an average one, and an average one above a weak one")
    void ranksTheSampleSubmissionsInOrder(String slug) throws IOException {
        ProblemDetail problem = problems.detailBySlug(slug);
        RubricView rubric = problems.rubricFor(problem.id());

        List<Integer> scores = new ArrayList<>();
        for (String quality : List.of("strong", "average", "weak")) {
            AiReviewResult result =
                    reviewer.reviewDesign(requestFor(problem, rubric, read(slug + "-" + quality + ".md")));
            assertThat(result.succeeded())
                    .as("the model should return a usable review for the %s %s submission", quality, slug)
                    .isTrue();
            int score = result.evaluation().orElseThrow().overallScore();
            log.info("eval {} {} scored {} in {} attempt(s)", slug, quality, score, result.attempts());
            scores.add(score);
        }

        int strong = scores.get(0);
        int average = scores.get(1);
        int weak = scores.get(2);

        // What the reviewer is held to: it must clearly separate a strong design from a
        // weak one, and must never rank a strong design below an average one.
        assertThat(strong - weak)
                .as("a strong design (%d) should beat a weak one (%d) by at least %d", strong, weak, REQUIRED_MARGIN)
                .isGreaterThanOrEqualTo(REQUIRED_MARGIN);
        assertThat(strong)
                .as("strong (%d) should not score below average (%d)", strong, average)
                .isGreaterThanOrEqualTo(average);

        // Average versus weak is reported, not asserted - see the note on this class.
        if (average < weak) {
            log.warn(
                    "eval {}: average ({}) scored below weak ({}); the middle band is not separated",
                    slug,
                    average,
                    weak);
        }
    }

    private DesignReviewRequest requestFor(ProblemDetail problem, RubricView rubric, String design) {
        List<String> requirements = new ArrayList<>(problem.functionalRequirements());
        requirements.addAll(problem.nonFunctionalRequirements());
        List<RubricDimensionPrompt> dimensions = rubric.dimensions().stream()
                .map(dimension ->
                        new RubricDimensionPrompt(dimension.name(), dimension.description(), dimension.mustHaves()))
                .toList();
        return new DesignReviewRequest(
                problem.title(),
                problem.statementMarkdown(),
                requirements,
                problem.scaleHints(),
                dimensions,
                design,
                "");
    }

    private static String read(String fileName) throws IOException {
        try (var input = new ClassPathResource("evals/" + fileName).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
