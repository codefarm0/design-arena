package in.codefarm.designarena.ai;

import java.util.List;

/**
 * A review of one design, as structured data rather than prose.
 *
 * <p>This record is the schema. Spring AI derives a JSON schema from it, asks the model to
 * answer in that shape, and maps the reply back — so no code anywhere parses model output
 * by hand, and a reply that does not fit this shape is a failure we can detect and retry
 * rather than a string somebody has to pick apart.
 *
 * @param overallScore the headline score out of ten
 * @param dimensions a score per rubric dimension
 * @param strengths what the design gets right
 * @param issues what is wrong or risky
 * @param missing what a staff-level answer should have covered and did not
 * @param followUpQuestions what an interviewer would ask next
 */
public record DesignEvaluation(
        int overallScore,
        List<DimensionScore> dimensions,
        List<String> strengths,
        List<String> issues,
        List<String> missing,
        List<String> followUpQuestions) {

    public DesignEvaluation {
        // A model that leaves a list out entirely is being unhelpful, not broken: an
        // empty list renders as an empty section. Missing scores are a different matter
        // and are rejected by the caller, which retries.
        dimensions = copyOrEmpty(dimensions);
        strengths = copyOrEmpty(strengths);
        issues = copyOrEmpty(issues);
        missing = copyOrEmpty(missing);
        followUpQuestions = copyOrEmpty(followUpQuestions);
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
