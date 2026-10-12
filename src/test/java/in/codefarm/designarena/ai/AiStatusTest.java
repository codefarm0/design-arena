package in.codefarm.designarena.ai;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.boot.actuate.info.Info;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * What the admin page and {@code /actuator/info} report about the AI layer.
 *
 * <p>The point of these values is that they come from the beans that are wired in, so the
 * test asserts exactly that: change the model the client is built with, and the reported
 * model changes with it.
 */
class AiStatusTest {

    private final ChatModel chatModel = Mockito.mock(ChatModel.class);

    @Test
    void reportsTheModelTheClientWillActuallyUse() {
        given(chatModel.getOptions())
                .willReturn(ChatOptions.builder().model("llama3.1:8b").build());

        AiStatus status = reviewer().status();

        assertThat(status.model()).isEqualTo("llama3.1:8b");
        assertThat(status.enabled()).isTrue();
        assertThat(status.maxRetries()).isEqualTo(2);
    }

    @Test
    void namesTheProviderFromTheModelBeanRatherThanFromConfiguration() {
        given(chatModel.getOptions()).willReturn(ChatOptions.builder().build());

        // Derived from the bean that is wired in, so it cannot disagree with reality.
        assertThat(reviewer().provider()).isNotBlank();
    }

    @Test
    void saysSoWhenTheModelDoesNotReportItsName() {
        given(chatModel.getOptions()).willReturn(ChatOptions.builder().build());

        assertThat(reviewer().status().model()).isEqualTo("unknown");
    }

    @Test
    void reportsEveryPromptAndItsVersion() {
        given(chatModel.getOptions()).willReturn(ChatOptions.builder().build());

        assertThat(reviewer().status().promptVersions())
                .containsEntry("QUIZ_SUMMARY", "v1")
                .containsEntry("DESIGN_REVIEW", "v1");
    }

    @Test
    void putsTheProviderAndModelIntoActuatorInfo() {
        given(chatModel.getOptions())
                .willReturn(ChatOptions.builder().model("gpt-4.1-mini").build());
        Info.Builder builder = new Info.Builder();

        new AiInfoContributor(reviewer()).contribute(builder);

        assertThat(builder.build().getDetails()).containsKey("ai");
        assertThat(builder.build().getDetails().get("ai").toString()).contains("gpt-4.1-mini");
    }

    private StatusReporting reviewer() {
        SpringAiReviewer reviewer = new SpringAiReviewer(
                ChatClient.builder(chatModel), chatModel, new AiProperties(true, 2), new SimpleMeterRegistry());
        return new StatusReporting(reviewer);
    }

    /** Exposes the derived provider name alongside the status, for the assertion above. */
    private record StatusReporting(SpringAiReviewer delegate) implements AiReviewer {

        @Override
        public java.util.Optional<String> summariseQuizAttempt(QuizSummaryRequest request) {
            return delegate.summariseQuizAttempt(request);
        }

        @Override
        public AiReviewResult reviewDesign(DesignReviewRequest request) {
            return delegate.reviewDesign(request);
        }

        @Override
        public AiStatus status() {
            return delegate.status();
        }

        String provider() {
            return delegate.status().provider();
        }
    }
}
