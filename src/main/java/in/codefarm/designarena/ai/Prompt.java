package in.codefarm.designarena.ai;

/**
 * The prompts this application sends, each with a version.
 *
 * <p>Prompts live in resource files rather than Java strings for two reasons: they are
 * edited far more often than the code around them, and a diff of a prompt is readable.
 * The version travels with every result so you can tell which wording produced it — when
 * a review looks wrong six weeks from now, that is the first thing you need to know.
 */
enum Prompt {
    QUIZ_SUMMARY("prompts/quiz-summary.st", "v1"),
    DESIGN_REVIEW("prompts/design-review.st", "v1");

    private final String resource;
    private final String version;

    Prompt(String resource, String version) {
        this.resource = resource;
        this.version = version;
    }

    String resource() {
        return resource;
    }

    String version() {
        return version;
    }
}
