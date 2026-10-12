package in.codefarm.designarena.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the AI layer.
 *
 * <p>Note what is <em>not</em> here: which provider to use, which model, and the request
 * timeout. Those are Spring AI's and Spring's own properties
 * ({@code spring.ai.model.chat}, {@code spring.ai.*.chat.options.model},
 * {@code spring.http.client.read-timeout}), and duplicating them would create two places
 * to change one thing.
 *
 * @param enabled whether to call a model at all. Turning it off makes every AI feature
 *     fall back to its no-model behaviour, which is how the application runs in CI and on
 *     a machine that has not pulled a model yet.
 * @param maxRetries how many extra attempts to allow when a model returns something that
 *     is not valid against the schema we asked for. Small models get JSON wrong often
 *     enough that one or two retries turn most failures into successes; more than that
 *     just makes the user wait longer for the same outcome.
 */
@ConfigurationProperties("designarena.ai")
public record AiProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("2") int maxRetries) {}
