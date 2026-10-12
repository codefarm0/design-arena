package in.codefarm.designarena.problem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The list of problems a user can practise. */
@Service
public class ProblemCatalogue {

    private final ProblemRepository problems;
    private final RubricRepository rubrics;

    ProblemCatalogue(ProblemRepository problems, RubricRepository rubrics) {
        this.problems = problems;
        this.rubrics = rubrics;
    }

    /**
     * Every active problem, in the order they should be shown.
     *
     * <p>The transaction is read-only and ends with this method, which is why the result is
     * a list of records: nothing lazily loaded escapes to the caller.
     */
    @Transactional(readOnly = true)
    public List<ProblemSummary> activeProblems() {
        return problems.findActiveSummaries();
    }

    /**
     * One problem by its URL identifier.
     *
     * @throws ProblemNotFoundException if no active problem has that slug, which means a
     *     hand-edited or stale URL rather than a bug
     */
    @Transactional(readOnly = true)
    public ProblemSummary bySlug(String slug) {
        return problems.findActiveSummaryBySlug(slug).orElseThrow(() -> new ProblemNotFoundException(slug));
    }

    /**
     * One problem by identity, for features that hold only the id, such as a stored quiz
     * attempt.
     *
     * @throws ProblemNotFoundException if the problem no longer exists
     */
    @Transactional(readOnly = true)
    public ProblemSummary byId(long id) {
        return problems.findSummaryById(id).orElseThrow(() -> new ProblemNotFoundException("id " + id));
    }

    /**
     * Everything the design editor shows: the statement, both requirement lists and the
     * scale hints.
     *
     * <p>The lazy collections are read here, inside the transaction, and copied into a
     * record. Nothing lazily loaded escapes this method.
     */
    @Transactional(readOnly = true)
    public ProblemDetail detailBySlug(String slug) {
        Problem problem =
                problems.findBySlugAndActiveIsTrue(slug).orElseThrow(() -> new ProblemNotFoundException(slug));
        return new ProblemDetail(
                problem.getId(),
                problem.getSlug(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getStatementMarkdown(),
                problem.requirementsOfKind(RequirementKind.FUNCTIONAL),
                problem.requirementsOfKind(RequirementKind.NON_FUNCTIONAL),
                problem.scaleHintTexts());
    }

    /**
     * The rubric a design for this problem is judged against.
     *
     * @throws ProblemNotFoundException if the problem has no rubric, which would mean the
     *     seed data is incomplete rather than that the user did anything wrong
     */
    @Transactional(readOnly = true)
    public RubricView rubricFor(long problemId) {
        Rubric rubric = rubrics.findByProblemId(problemId)
                .orElseThrow(() -> new ProblemNotFoundException("rubric for id " + problemId));
        List<RubricDimensionView> dimensions = rubric.getDimensions().stream()
                .map(dimension -> new RubricDimensionView(
                        dimension.getName(),
                        dimension.getDescription(),
                        dimension.getWeight(),
                        dimension.getMustHaves().stream()
                                .map(RubricMustHave::getText)
                                .toList()))
                .toList();
        return new RubricView(dimensions);
    }
}
