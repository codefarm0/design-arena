package in.codefarm.designarena.ai;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The only class in the application that imports Spring AI.
 *
 * <p>Everything vendor-specific stops here: which provider is configured, how a prompt
 * template is rendered, what the client throws when a model is slow. Callers see an
 * {@link Optional}.
 */
@Service
class SpringAiReviewer implements AiReviewer {

    private static final Logger log = LoggerFactory.getLogger(SpringAiReviewer.class);

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final AiProperties properties;
    private final MeterRegistry meters;
    private final String provider;

    SpringAiReviewer(
            ChatClient.Builder chatClientBuilder, ChatModel chatModel, AiProperties properties, MeterRegistry meters) {
        this.chatClient = chatClientBuilder.build();
        this.chatModel = chatModel;
        this.properties = properties;
        this.meters = meters;
        // Derived from the model bean that is actually wired in, rather than read from a
        // property: there is then no second place for the provider name to drift out of
        // step with the provider doing the work.
        this.provider =
                chatModel.getClass().getSimpleName().replace("ChatModel", "").toLowerCase(Locale.ROOT);
    }

    @Override
    public Optional<String> summariseQuizAttempt(QuizSummaryRequest request) {
        if (!properties.enabled()) {
            log.debug("AI is disabled; skipping the quiz summary.");
            return Optional.empty();
        }

        PromptTemplate template = new PromptTemplate(new ClassPathResource(Prompt.QUIZ_SUMMARY.resource()));
        String prompt = template.render(Map.of(
                "problemTitle", request.problemTitle(),
                "score", request.score(),
                "maxScore", request.maxScore(),
                "decisions", String.join("\n", request.decisions())));

        return call(Prompt.QUIZ_SUMMARY, prompt);
    }

    @Override
    public AiStatus status() {
        return new AiStatus(
                provider,
                modelName(),
                properties.enabled(),
                properties.maxRetries(),
                Arrays.stream(Prompt.values()).collect(Collectors.toMap(Enum::name, Prompt::version)));
    }

    /** The model the client will actually use, read from the active model's own options. */
    private String modelName() {
        ChatOptions options = chatModel.getOptions();
        String model = options == null ? null : options.getModel();
        return model == null || model.isBlank() ? "unknown" : model;
    }

    @Override
    public AiReviewResult reviewDesign(DesignReviewRequest request) {
        BeanOutputConverter<DesignEvaluation> converter = new BeanOutputConverter<>(DesignEvaluation.class);
        String prompt = new PromptTemplate(new ClassPathResource(Prompt.DESIGN_REVIEW.resource()))
                .render(Map.of(
                        "problemTitle", request.problemTitle(),
                        "problemStatement", request.problemStatement(),
                        "requirements", bullets(request.requirements()),
                        "scaleHints", bullets(request.scaleHints()),
                        "rubric", describeRubric(request.rubricDimensions()),
                        "designMarkdown", request.designMarkdown(),
                        "diagramMermaid",
                                request.diagramMermaid().isBlank() ? "(no diagram provided)" : request.diagramMermaid(),
                        // The schema the reply must match, generated from the record itself.
                        "format", converter.getFormat()));

        if (!properties.enabled()) {
            return failed("", 0, 0, "AI is disabled by configuration.");
        }
        return callWithRetries(prompt, converter);
    }

    /**
     * Asks for a review, and asks again when the reply does not fit the schema.
     *
     * <p>Retrying is worth it because the usual failure of a small model is a near miss:
     * prose wrapped around the JSON, a trailing comma, a missing field. Retrying is also
     * bounded, because a model that cannot produce the shape twice will not produce it on
     * the tenth attempt either — at that point the honest answer to the user is that the
     * review failed, with a button to try again.
     */
    private AiReviewResult callWithRetries(String prompt, BeanOutputConverter<DesignEvaluation> converter) {
        long startedAt = System.nanoTime();
        int maxAttempts = Math.max(1, properties.maxRetries() + 1);
        String raw = "";
        String failure = "The model did not return a usable review.";
        ChatResponse lastResponse = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                lastResponse = chatClient.prompt().user(prompt).call().chatResponse();
                raw = textOf(lastResponse);
                DesignEvaluation evaluation = converter.convert(raw);
                reject(evaluation);
                record("success", attempt);
                return new AiReviewResult(
                        Optional.of(evaluation),
                        raw,
                        provider,
                        modelOf(lastResponse),
                        Prompt.DESIGN_REVIEW.version(),
                        millisSince(startedAt),
                        attempt,
                        promptTokensOf(lastResponse),
                        completionTokensOf(lastResponse),
                        Optional.empty());
            } catch (RuntimeException e) {
                failure = e.getClass().getSimpleName() + ": " + e.getMessage();
                log.warn("Review attempt {} of {} was unusable: {}", attempt, maxAttempts, failure);
            }
        }
        record("failure", maxAttempts);
        return new AiReviewResult(
                Optional.empty(),
                raw,
                provider,
                lastResponse == null ? "unknown" : modelOf(lastResponse),
                Prompt.DESIGN_REVIEW.version(),
                millisSince(startedAt),
                maxAttempts,
                promptTokensOf(lastResponse),
                completionTokensOf(lastResponse),
                Optional.of(truncate(failure)));
    }

    /**
     * Rejects a reply that parsed but says nothing usable.
     *
     * <p>Valid JSON is not the same as a valid review. A model that returns an empty
     * dimension list or a score of 42 has failed, and failing here means the retry loop
     * gets another go rather than the user getting nonsense.
     */
    private static void reject(DesignEvaluation evaluation) {
        if (evaluation == null) {
            throw new IllegalStateException("the reply could not be read as a review");
        }
        if (evaluation.overallScore() < 0 || evaluation.overallScore() > 10) {
            throw new IllegalStateException("overall score " + evaluation.overallScore() + " is outside 0-10");
        }
        if (evaluation.dimensions().isEmpty()) {
            throw new IllegalStateException("no dimension scores were returned");
        }
        evaluation.dimensions().forEach(dimension -> {
            if (dimension.score() < 0 || dimension.score() > 10) {
                throw new IllegalStateException(
                        "score " + dimension.score() + " for " + dimension.dimension() + " is outside 0-10");
            }
        });
    }

    private AiReviewResult failed(String raw, long latencyMs, int attempts, String reason) {
        return new AiReviewResult(
                Optional.empty(),
                raw,
                provider,
                "none",
                Prompt.DESIGN_REVIEW.version(),
                latencyMs,
                attempts,
                Optional.empty(),
                Optional.empty(),
                Optional.of(reason));
    }

    /**
     * Token counts, when the provider reports them.
     *
     * <p>Ollama reports them; some OpenAI-compatible endpoints do not. Recording them is
     * what makes it possible to answer "what does a review cost" later without guessing,
     * so they are stored whenever they are offered and left absent when they are not.
     */
    private static Optional<Integer> promptTokensOf(ChatResponse response) {
        return usageOf(response).map(Usage::getPromptTokens);
    }

    private static Optional<Integer> completionTokensOf(ChatResponse response) {
        return usageOf(response).map(Usage::getCompletionTokens);
    }

    private static Optional<Usage> usageOf(ChatResponse response) {
        if (response == null || response.getMetadata() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(response.getMetadata().getUsage());
    }

    private static String bullets(List<String> values) {
        return values.stream().map(value -> "- " + value).collect(java.util.stream.Collectors.joining("\n"));
    }

    private static String describeRubric(List<RubricDimensionPrompt> dimensions) {
        StringBuilder text = new StringBuilder();
        for (RubricDimensionPrompt dimension : dimensions) {
            text.append("\n")
                    .append(dimension.name())
                    .append(": ")
                    .append(dimension.description())
                    .append("\n");
            dimension
                    .mustHaves()
                    .forEach(mustHave -> text.append("  - ").append(mustHave).append("\n"));
        }
        return text.toString();
    }

    private static String textOf(ChatResponse response) {
        return response == null || response.getResult() == null
                ? ""
                : response.getResult().getOutput().getText();
    }

    private static String modelOf(ChatResponse response) {
        String model =
                response.getMetadata() == null ? null : response.getMetadata().getModel();
        return model == null || model.isBlank() ? "unknown" : model;
    }

    private static long millisSince(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }

    private static String truncate(String reason) {
        return reason.length() <= 400 ? reason : reason.substring(0, 397) + "...";
    }

    private void record(String outcome, int attempts) {
        meters.counter(
                        "designarena.ai.review",
                        "outcome",
                        outcome,
                        "version",
                        Prompt.DESIGN_REVIEW.version(),
                        "attempts",
                        String.valueOf(attempts))
                .increment();
    }

    /**
     * Sends one prompt and records how it went.
     *
     * <p>Any failure returns empty rather than propagating. The request timeout itself is
     * the HTTP client's ({@code spring.http.client.read-timeout}), so a model that never
     * answers releases the connection instead of holding a thread indefinitely.
     *
     * <p>Catching broadly is deliberate here and nowhere else: this is a boundary to a
     * remote system whose client libraries throw a wide range of unchecked exceptions, and
     * the entire contract of this class is that the caller's page still renders. The
     * failure is logged and counted, never swallowed silently.
     */
    private Optional<String> call(Prompt prompt, String renderedPrompt) {
        Timer.Sample sample = Timer.start(meters);
        String outcome = "success";
        try {
            String answer = chatClient.prompt().user(renderedPrompt).call().content();
            if (answer == null || answer.isBlank()) {
                outcome = "empty";
                log.warn("Model returned an empty response for prompt {} {}", prompt, prompt.version());
                return Optional.empty();
            }
            return Optional.of(answer.strip());
        } catch (RuntimeException e) {
            outcome = "failure";
            log.warn("Model call failed for prompt {} {}: {}", prompt, prompt.version(), e.getMessage());
            return Optional.empty();
        } finally {
            sample.stop(meters.timer(
                    "designarena.ai.call", "prompt", prompt.name(), "version", prompt.version(), "outcome", outcome));
        }
    }
}
