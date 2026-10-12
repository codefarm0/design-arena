package in.codefarm.designarena.problem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Editing the catalogue: problems, their requirements, and their rubrics.
 *
 * <p>Seed data put the first three problems in through Flyway, which is right for content
 * that ships with the application. This is for everything after that, and it is the only
 * write path into the catalogue outside a migration.
 */
@Service
public class ProblemAdmin {

    private final ProblemAdminRepository problems;
    private final RubricRepository rubrics;

    ProblemAdmin(ProblemAdminRepository problems, RubricRepository rubrics) {
        this.problems = problems;
        this.rubrics = rubrics;
    }

    /** Every problem, including the ones not shown in the catalogue. */
    @Transactional(readOnly = true)
    public List<ProblemSummary> all() {
        return problems.findAllByOrderByPositionAsc().stream()
                .map(problem -> new ProblemSummary(
                        problem.getId(),
                        problem.getSlug(),
                        problem.getTitle(),
                        problem.getDifficulty(),
                        problem.getBlurb()))
                .toList();
    }

    /** Whether a problem is currently in the catalogue. */
    @Transactional(readOnly = true)
    public boolean isActive(String slug) {
        return find(slug).isActive();
    }

    /** The current content of a problem, ready to populate the edit form. */
    @Transactional(readOnly = true)
    public ProblemDraft draftOf(String slug) {
        Problem problem = find(slug);
        return new ProblemDraft(
                problem.getSlug(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getBlurb(),
                problem.getStatementMarkdown(),
                problem.requirementsOfKind(RequirementKind.FUNCTIONAL),
                problem.requirementsOfKind(RequirementKind.NON_FUNCTIONAL),
                problem.scaleHintTexts(),
                problem.isActive());
    }

    /**
     * Creates an empty problem.
     *
     * <p>It starts inactive, so a half-written problem is never visible to anybody
     * practising. Publishing is a separate, deliberate action.
     */
    @Transactional
    public void create(String slug) {
        String normalised = slug.strip().toLowerCase(java.util.Locale.ROOT);
        if (problems.findBySlug(normalised).isPresent()) {
            throw new SlugAlreadyUsedException(normalised);
        }
        int nextPosition = problems.findAllByOrderByPositionAsc().size() + 1;
        Problem problem = problems.save(new Problem(normalised, nextPosition));
        rubrics.save(new Rubric(problem));
    }

    /** Saves an edit, replacing the requirement and scale-hint lists wholesale. */
    @Transactional
    public void save(ProblemDraft draft) {
        Problem problem = find(draft.slug());
        problem.apply(draft);

        List<ProblemRequirement> requirements = new ArrayList<>();
        addRequirements(problem, requirements, RequirementKind.FUNCTIONAL, draft.functionalRequirements());
        addRequirements(problem, requirements, RequirementKind.NON_FUNCTIONAL, draft.nonFunctionalRequirements());
        problem.replaceRequirements(requirements);

        List<ProblemScaleHint> hints = new ArrayList<>();
        for (int i = 0; i < draft.scaleHints().size(); i++) {
            hints.add(new ProblemScaleHint(problem, i + 1, draft.scaleHints().get(i)));
        }
        problem.replaceScaleHints(hints);

        problems.save(problem);
    }

    /** The current rubric, ready to populate the edit form. */
    @Transactional(readOnly = true)
    public RubricDraft rubricDraftOf(String slug) {
        Problem problem = find(slug);
        Rubric rubric = rubrics.findByProblemId(problem.getId())
                .orElseThrow(() -> new ProblemNotFoundException("rubric for " + slug));
        List<RubricDimensionDraft> dimensions = rubric.getDimensions().stream()
                .map(dimension -> new RubricDimensionDraft(
                        dimension.getName(),
                        dimension.getDescription(),
                        dimension.getWeight(),
                        dimension.getMustHaves().stream()
                                .map(RubricMustHave::getText)
                                .toList()))
                .toList();
        return new RubricDraft(dimensions);
    }

    /**
     * Replaces a rubric wholesale.
     *
     * <p>Replacing rather than merging is the honest operation here: dimensions are an
     * ordered list that the form edits as a whole, and matching old rows to new ones by
     * name would silently lose an edit that renamed one.
     *
     * <p>Evaluations already produced are untouched. They store their result as JSON with
     * the dimension names the model was given at the time, so editing a rubric never
     * rewrites history.
     */
    @Transactional
    public void saveRubric(String slug, RubricDraft draft) {
        Problem problem = find(slug);
        Rubric rubric = rubrics.findByProblemId(problem.getId())
                .orElseThrow(() -> new ProblemNotFoundException("rubric for " + slug));

        List<RubricDimension> dimensions = new ArrayList<>();
        for (int i = 0; i < draft.dimensions().size(); i++) {
            RubricDimensionDraft source = draft.dimensions().get(i);
            RubricDimension dimension = new RubricDimension(
                    rubric, i + 1, source.name(), source.description(), Math.max(1, source.weight()));
            List<RubricMustHave> mustHaves = new ArrayList<>();
            for (int j = 0; j < source.mustHaves().size(); j++) {
                mustHaves.add(
                        new RubricMustHave(dimension, j + 1, source.mustHaves().get(j)));
            }
            dimension.replaceMustHaves(mustHaves);
            dimensions.add(dimension);
        }
        rubric.replaceDimensions(dimensions);
        rubrics.save(rubric);
    }

    private static void addRequirements(
            Problem problem, List<ProblemRequirement> target, RequirementKind kind, List<String> texts) {
        for (int i = 0; i < texts.size(); i++) {
            target.add(new ProblemRequirement(problem, kind, i + 1, texts.get(i)));
        }
    }

    private Problem find(String slug) {
        return problems.findBySlug(slug).orElseThrow(() -> new ProblemNotFoundException(slug));
    }
}
