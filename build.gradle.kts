import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    application
    java
}

group = "alma"
version = "0.1.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass = "alma.Main"
}

dependencies {
    // Core agent framework.
    implementation(libs.langchain4j.openai)

    // Terminal UI.
    implementation(libs.picocli)
    annotationProcessor(libs.picocli.codegen)

    // NLP and language detection.
    implementation(libs.opennlp)
    implementation(libs.languageDetector)
    implementation(libs.icu4j)

    // JSON.
    implementation(libs.jackson.databind)
    implementation(libs.jackson.parameterNames)

    // Logging.
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)

    // Testing.
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platformLauncher)
    testImplementation(libs.assertj)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// Replace `@version@` token in alma.properties with project.version.
tasks.processResources {
    filesMatching("alma.properties") {
        filter<ReplaceTokens>("tokens" to mapOf("version" to project.version.toString()))
    }
}

// Runs the real EvalHarness via the ALMA CLI's `evaluate` subcommand.
tasks.register<JavaExec>("evaluate") {
    description = "Run ALMA eval harness on the bundled benchmark."
    group = "verification"
    mainClass.set("alma.Main")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("evaluate")
    standardInput = System.`in`
}
