package in.codefarm.designarena.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/** Where slow work runs, so it never happens on a thread that is serving a page. */
@Configuration(proxyBeanMethods = false)
class BackgroundWorkConfiguration {

    /**
     * The pool that calls language models.
     *
     * <p>Deliberately small and deliberately bounded. A local model answers in tens of
     * seconds, so an unbounded pool would let a burst of submissions open a hundred slow
     * connections at once. When the queue is full the caller runs the task itself, which
     * slows submissions down rather than dropping them.
     */
    @Bean
    Executor summaryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-summary-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
