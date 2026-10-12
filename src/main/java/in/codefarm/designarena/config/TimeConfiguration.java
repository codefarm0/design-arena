package in.codefarm.designarena.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The clock, as an injected dependency rather than a static call.
 *
 * <p>Code that calls {@code Instant.now()} directly cannot be tested without waiting for
 * real time to pass. Injecting the clock means a test can fix it.
 */
@Configuration(proxyBeanMethods = false)
class TimeConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
