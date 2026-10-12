package in.codefarm.designarena;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real infrastructure for tests that need it.
 *
 * <p>The Postgres image is pinned to the same version as compose.yaml, so a migration
 * that passes here behaves identically when the application runs locally.
 *
 * <p>There is no Ollama container yet: nothing in this milestone calls a model, and
 * starting one would add minutes to every test run for no assertion.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"));
    }
}
