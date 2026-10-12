package in.codefarm.designarena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Application entry point. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DesignArenaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DesignArenaApplication.class, args);
    }
}
