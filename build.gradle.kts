plugins {
    application
    id("com.gradleup.shadow") version "9.0.0"
}

group = "com.aggregator"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.4")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("org.apache.logging.log4j:log4j-core:2.25.4")
    runtimeOnly("org.apache.logging.log4j:log4j-api:2.26.1")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")
}
