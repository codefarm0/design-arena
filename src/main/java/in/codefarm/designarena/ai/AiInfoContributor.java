package in.codefarm.designarena.ai;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Puts the active provider and model into {@code /actuator/info}.
 *
 * <p>So that the answer to "what is this deployment actually running?" comes from the
 * deployment itself, rather than from somebody reading the YAML they think it has.
 */
@Component
class AiInfoContributor implements InfoContributor {

    private final AiReviewer reviewer;

    AiInfoContributor(AiReviewer reviewer) {
        this.reviewer = reviewer;
    }

    @Override
    public void contribute(Info.Builder builder) {
        AiStatus status = reviewer.status();
        builder.withDetail(
                "ai",
                Map.of(
                        "provider", status.provider(),
                        "model", status.model(),
                        "enabled", status.enabled(),
                        "maxRetries", status.maxRetries(),
                        "promptVersions", status.promptVersions()));
    }
}
