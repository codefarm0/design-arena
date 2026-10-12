package in.codefarm.designarena;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * The application starts: every bean wires up, Flyway migrates, and Hibernate validates
 * its mapping against the migrated schema.
 *
 * <p>This is the test that catches a forgotten migration or a mapping that drifted from
 * the schema, because {@code ddl-auto: validate} fails startup rather than guessing.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DesignArenaApplicationTests {

    @Test
    void contextLoads() {
        // Startup is the assertion.
    }
}
