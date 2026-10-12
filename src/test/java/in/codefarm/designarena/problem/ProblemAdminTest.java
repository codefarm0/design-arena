package in.codefarm.designarena.problem;

import in.codefarm.designarena.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Editing the catalogue, against the real schema and the real seed data. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, ProblemAdmin.class, ProblemCatalogue.class})
class ProblemAdminTest {

    private final ProblemAdmin admin;
    private final ProblemCatalogue catalogue;

    ProblemAdminTest(ProblemAdmin admin, ProblemCatalogue catalogue) {
        this.admin = admin;
        this.catalogue = catalogue;
    }

    @Test
    void listsEverySeededProblem() {
        assertThat(admin.all())
                .extracting(ProblemSummary::slug)
                .contains("rate-limiter", "url-shortener", "notification-system");
    }

    @Test
    void createsAProblemUnpublished() {
        admin.create("message-queue");

        assertThat(admin.isActive("message-queue")).isFalse();
        // A half-written problem must never appear to somebody practising.
        assertThat(catalogue.activeProblems()).extracting(ProblemSummary::slug).doesNotContain("message-queue");
    }

    @Test
    void normalisesANewSlug() {
        admin.create("  Message-Queue  ");

        assertThat(admin.draftOf("message-queue").slug()).isEqualTo("message-queue");
    }

    @Test
    void refusesASlugThatIsAlreadyUsed() {
        assertThatThrownBy(() -> admin.create("rate-limiter")).isInstanceOf(SlugAlreadyUsedException.class);
    }

    @Test
    void savesContentAndPublishesIt() {
        admin.create("message-queue");

        admin.save(new ProblemDraft(
                "message-queue",
                "Design a Message Queue",
                Difficulty.HARD,
                "Deliver messages in order, at least once, at scale.",
                "Design a durable queue other services publish to and consume from.",
                List.of("Publish a message", "Consume in order"),
                List.of("Survives a broker restart"),
                List.of("1M messages/sec"),
                true));

        ProblemDetail detail = catalogue.detailBySlug("message-queue");
        assertThat(detail.title()).isEqualTo("Design a Message Queue");
        assertThat(detail.difficulty()).isEqualTo(Difficulty.HARD);
        assertThat(detail.functionalRequirements()).containsExactly("Publish a message", "Consume in order");
        assertThat(detail.nonFunctionalRequirements()).containsExactly("Survives a broker restart");
        assertThat(detail.scaleHints()).containsExactly("1M messages/sec");
        assertThat(catalogue.activeProblems()).extracting(ProblemSummary::slug).contains("message-queue");
    }

    @Test
    void replacesRequirementListsRatherThanAppending() {
        ProblemDraft original = admin.draftOf("rate-limiter");
        assertThat(original.functionalRequirements()).hasSizeGreaterThan(1);

        admin.save(new ProblemDraft(
                original.slug(),
                original.title(),
                original.difficulty(),
                original.blurb(),
                original.statementMarkdown(),
                List.of("Only this one"),
                List.of(),
                original.scaleHints(),
                original.active()));

        ProblemDetail detail = catalogue.detailBySlug("rate-limiter");
        assertThat(detail.functionalRequirements()).containsExactly("Only this one");
        assertThat(detail.nonFunctionalRequirements()).isEmpty();
    }

    @Test
    void unpublishingTakesAProblemOutOfTheCatalogueWithoutDeletingIt() {
        ProblemDraft draft = admin.draftOf("url-shortener");

        admin.save(new ProblemDraft(
                draft.slug(),
                draft.title(),
                draft.difficulty(),
                draft.blurb(),
                draft.statementMarkdown(),
                draft.functionalRequirements(),
                draft.nonFunctionalRequirements(),
                draft.scaleHints(),
                false));

        assertThat(catalogue.activeProblems()).extracting(ProblemSummary::slug).doesNotContain("url-shortener");
        // Still there, so history and old links are not broken by unpublishing.
        assertThat(admin.all()).extracting(ProblemSummary::slug).contains("url-shortener");
    }

    @Test
    void editsARubricWholesale() {
        RubricDraft original = admin.rubricDraftOf("rate-limiter");
        assertThat(original.dimensions()).hasSize(7);

        admin.saveRubric(
                "rate-limiter",
                new RubricDraft(List.of(
                        new RubricDimensionDraft(
                                "Correctness", "Does it work at all", 3, List.of("Names an algorithm")),
                        new RubricDimensionDraft("Operability", "Can it be run", 1, List.of()))));

        RubricView rubric = catalogue.rubricFor(admin.all().stream()
                .filter(problem -> problem.slug().equals("rate-limiter"))
                .findFirst()
                .orElseThrow()
                .id());
        assertThat(rubric.dimensions())
                .extracting(RubricDimensionView::name)
                .containsExactly("Correctness", "Operability");
        assertThat(rubric.dimensions().getFirst().weight()).isEqualTo(3);
        assertThat(rubric.dimensions().getFirst().mustHaves()).containsExactly("Names an algorithm");
        assertThat(rubric.dimensions().getLast().mustHaves()).isEmpty();
    }

    @Test
    void givesANewProblemAnEmptyRubricToFillIn() {
        admin.create("message-queue");

        assertThat(admin.rubricDraftOf("message-queue").dimensions()).isEmpty();
    }

    @Test
    void rejectsAWeightBelowOne() {
        admin.saveRubric(
                "rate-limiter", new RubricDraft(List.of(new RubricDimensionDraft("Only", "One", 0, List.of()))));

        // A zero weight would remove the dimension from the weighted mean silently.
        assertThat(admin.rubricDraftOf("rate-limiter").dimensions().getFirst().weight())
                .isEqualTo(1);
    }
}
