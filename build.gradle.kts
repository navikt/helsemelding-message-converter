plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.1.10"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("com.gradleup.shadow") version "8.3.6"
    id("maven-publish")
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.arrow.functions)
    implementation(libs.bundles.logging)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.json.schema.core)
    implementation(libs.javax.jaxb.api)
    implementation(libs.migesok.jaxb.time.adapters)
    implementation(libs.bundles.nav.xml)
    testImplementation(testLibs.bundles.kotest)
    testImplementation(testLibs.kotest.assertions.arrow)
    testImplementation(testLibs.kotest.extensions.jvm)
    testImplementation(testLibs.kotest.extensions.testcontainers)
    testImplementation(testLibs.testcontainers)
    testImplementation(testLibs.testcontainers.postgresql)
    testImplementation(kotlin("test"))
}

plugins.withId("org.jetbrains.kotlin.jvm") {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
plugins.withId("org.jlleitschuh.gradle.ktlint") {
    tasks.named("ktlintCheck") {
        mustRunAfter("ktlintFormat")
    }

    tasks.named("build") {
        dependsOn("ktlintCheck")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "no.nav.helsemelding"
            artifactId = "message-converter"
            version = "0.0.1"
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/navikt/${rootProject.name}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
