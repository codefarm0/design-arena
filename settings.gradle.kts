plugins {
    // Downloads the JDK pinned by the Java toolchain when the local machine does not
    // have it, so the build is reproducible on any developer or CI machine.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "design-arena"
