package in.codefarm.designarena.problem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * A system design problem a user can practise.
 *
 * <p>Package-private on purpose: entities never leave their feature package. Everything
 * outside {@code problem} sees {@link ProblemSummary} instead, so no other package can
 * accidentally depend on the database shape or trigger lazy loading.
 *
 * <p>Only the columns the application reads are mapped. The schema is owned by Flyway,
 * and Hibernate is configured to validate this mapping against it at startup.
 *
 * <p>The catalogue does not use the getters below: it projects columns straight into
 * {@link ProblemSummary}. They exist for the editor, which needs the whole problem,
 * including its requirement and scale-hint lists.
 */
@Entity
@Table(name = "problem")
class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(nullable = false)
    private String blurb;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "statement_md", nullable = false)
    private String statementMarkdown;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ProblemRequirement> requirements = new ArrayList<>();

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ProblemScaleHint> scaleHints = new ArrayList<>();

    protected Problem() {
        // Required by JPA.
    }

    Problem(String slug, int position) {
        this.slug = slug;
        this.position = position;
        this.active = false;
        this.title = slug;
        this.blurb = "";
        this.statementMarkdown = "";
        this.difficulty = Difficulty.MEDIUM;
    }

    /**
     * Applies an edit from the admin form.
     *
     * <p>The slug is not among these. Changing it would break every link to the problem
     * and every stored submission's route, so a renamed problem is a new problem.
     */
    void apply(ProblemDraft draft) {
        this.title = draft.title().strip();
        this.difficulty = draft.difficulty();
        this.blurb = draft.blurb().strip();
        this.statementMarkdown = draft.statementMarkdown().strip();
        this.active = draft.active();
    }

    void replaceRequirements(List<ProblemRequirement> replacements) {
        requirements.clear();
        requirements.addAll(replacements);
    }

    void replaceScaleHints(List<ProblemScaleHint> replacements) {
        scaleHints.clear();
        scaleHints.addAll(replacements);
    }

    List<ProblemRequirement> requirements() {
        return requirements;
    }

    List<ProblemScaleHint> scaleHints() {
        return scaleHints;
    }

    boolean isActive() {
        return active;
    }

    int getPosition() {
        return position;
    }

    Long getId() {
        return id;
    }

    String getSlug() {
        return slug;
    }

    String getTitle() {
        return title;
    }

    Difficulty getDifficulty() {
        return difficulty;
    }

    String getBlurb() {
        return blurb;
    }

    String getStatementMarkdown() {
        return statementMarkdown;
    }

    /** Requirement text of one kind, in display order. */
    List<String> requirementsOfKind(RequirementKind kind) {
        return requirements.stream()
                .filter(requirement -> requirement.getKind() == kind)
                .map(ProblemRequirement::getText)
                .toList();
    }

    List<String> scaleHintTexts() {
        return scaleHints.stream().map(ProblemScaleHint::getText).toList();
    }
}
