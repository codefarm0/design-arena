package in.codefarm.designarena.ai;

import java.util.Map;

/**
 * What the AI layer is actually configured to do, right now.
 *
 * <p>This exists because "which model answered?" is the first question anybody asks about
 * a surprising review, and reading it off a YAML file tells you what somebody intended
 * rather than what is running. These values come from the beans that are wired in.
 *
 * @param provider the provider doing the work, derived from the active model bean
 * @param model the model name it will use
 * @param enabled whether model calls are switched on at all
 * @param maxRetries extra attempts allowed when a reply does not match the schema
 * @param promptVersions each prompt this application sends, and its version
 */
public record AiStatus(
        String provider, String model, boolean enabled, int maxRetries, Map<String, String> promptVersions) {

    public AiStatus {
        promptVersions = Map.copyOf(promptVersions);
    }
}
