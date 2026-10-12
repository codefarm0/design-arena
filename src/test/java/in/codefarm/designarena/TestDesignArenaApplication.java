package in.codefarm.designarena;

import org.springframework.boot.SpringApplication;

public class TestDesignArenaApplication {

    public static void main(String[] args) {
        SpringApplication.from(DesignArenaApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
