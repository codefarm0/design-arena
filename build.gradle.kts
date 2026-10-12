plugins {
    java
    jacoco
    checkstyle
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.vaadin") version "25.3.1"
    // Not BOM-managed: Spotless is a build-only tool with its own release train.
    id("com.diffplug.spotless") version "8.9.0"
}

group = "in.codefarm"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        // Pinned to an LTS. The toolchain resolver fetches it if the machine lacks it,
        // so the bytecode does not change with whatever JDK happens to be on PATH.
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

extra["springAiVersion"] = "2.0.1"
extra["vaadinVersion"] = "25.3.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("com.vaadin:vaadin-spring-boot-starter")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.ai:spring-ai-starter-model-anthropic")
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    runtimeOnly("org.postgresql:postgresql")

    developmentOnly("com.vaadin:vaadin-dev")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    developmentOnly("org.springframework.ai:spring-ai-spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.ai:spring-ai-spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-ollama")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("com.vaadin:vaadin-bom:${property("vaadinVersion")}")
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

// ---------------------------------------------------------------------------
// Keep generated output away from hand-written code.
//
// Vaadin generates a frontend scaffold, an npm project and its dev/production bundles.
// Left at their defaults these land in src/main and in the project root, mixed in with
// code a person wrote. What can be moved is moved here; what cannot is git-ignored and
// documented in src/main/frontend/README.md.
// ---------------------------------------------------------------------------

vaadin {
    // Vaadin's own build directory, which otherwise appears in full as a second build
    // folder called "target" beside Gradle's own.
    projectBuildDir = "build"
}

// One Vaadin code path writes its dev bundle to "target" whatever the setting above
// says, so `clean` removes that directory too and it never lingers in a checkout.
tasks.clean {
    delete("target")
}

// Note on npmFolder: it stays at the project root. Vite resolves the generated
// index.html relative to it, so moving it under build/ breaks the production frontend
// build. The four generated entries it leaves at the root are git-ignored.

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // -parameters keeps parameter names in the bytecode, which Spring needs for binding.
    // Warnings are errors so lint findings cannot accumulate. Two categories are off:
    //  * processing - annotation-processor chatter from Spring's metadata generator.
    //  * serial     - every Vaadin component is Serializable, and a serialVersionUID on a
    //                 UI class is noise: sessions are never deserialised across versions.
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing,-serial", "-Werror", "-parameters"))
}

// ---------------------------------------------------------------------------
// Quality gates. Each of these blocks `check`, and therefore `build`, on failure.
// ---------------------------------------------------------------------------

spotless {
    java {
        // Palantir's formatter is deterministic and needs no per-project config,
        // so formatting never becomes a review topic.
        palantirJavaFormat("2.102.0")
        removeUnusedImports()
        importOrder("", "jakarta", "java", "javax", "\\#")
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("sql") {
        target("src/main/resources/db/migration/**/*.sql")
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("styles") {
        // Only our own stylesheets: src/main/frontend also holds Vaadin's generated
        // output, which is not ours to format and would create a task dependency on the
        // frontend build.
        target("src/main/frontend/styles/**/*.css")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

checkstyle {
    toolVersion = "14.3.0"
    configFile = file("config/checkstyle/checkstyle.xml")
    // Any violation fails the build. A rule we disagree with gets removed from the
    // ruleset in a reviewed commit, not silently tolerated.
    maxWarnings = 0
    isIgnoreFailures = false
}

jacoco {
    toolVersion = "0.8.14"
}

/**
 * Classes with no branching logic worth a coverage target: the Spring entry point and
 * the Vaadin views, which are verified by looking at them and by UI tests later.
 */
val coverageExclusions = listOf("in/codefarm/designarena/DesignArenaApplication.class", "in/codefarm/designarena/ui/**")

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) { exclude(coverageExclusions) }
            }
        )
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) { exclude(coverageExclusions) }
            }
        )
    )
}

tasks.test {
    useJUnitPlatform()
    // Forward the evals flag to the test JVM. Without it, `-Devals=true` would reach
    // Gradle and not the tests, and the eval set would stay silently disabled.
    systemProperty("evals", providers.systemProperty("evals").getOrElse("false"))
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
