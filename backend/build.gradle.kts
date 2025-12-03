plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotless)

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)

    java
    `maven-publish`
    application
}

group = "nl.wilcokas"
version = "7.0.0-beta"
description = "LuckyStackWorker"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

application {
    mainClass.set("nl.wilcokas.luckystackworker.LuckystackWorkerApplication")
}

repositories {
    mavenCentral()
    flatDir {
        dirs(
            "${System.getProperty("user.home")}/Library/ParallelColt",
            "${System.getProperty("user.home")}/Library"
        )
    }
}

dependencies {
    implementation(libs.org.springframework.boot.spring.boot.starter)
    implementation(libs.org.springframework.boot.spring.boot.starter.web)
    implementation(libs.commons.io.commons.io)
    implementation(libs.org.apache.velocity.velocity.engine.core)
    implementation(libs.org.apache.commons.commons.text)
    implementation(libs.net.imagej.ij)
    implementation(libs.org.yaml.snakeyaml)
    implementation(libs.com.fasterxml.jackson.datatype.jackson.datatype.jsr310)
    implementation(libs.org.apache.commons.math3)
    implementation(libs.net.sourceforge.parallelcolt)
    implementation(libs.io.github.stevenjwest.paralleliterativedeconvolution)
    implementation(libs.com.github.wendykierp.jtransforms)
    implementation(libs.org.apache.httpcomponents.client5)
    implementation(libs.sc.fiji.bunwarpj)
    implementation(libs.sc.fiji.turboreg)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)

    compileOnly(libs.org.projectlombok.lombok)
    annotationProcessor(libs.org.projectlombok.lombok)

    testCompileOnly(libs.org.projectlombok.lombok)
    testAnnotationProcessor(libs.org.projectlombok.lombok)

    // Testing
    testImplementation(libs.org.springframework.boot.spring.boot.starter.test)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Toegang tot buildDir is in recente Gradle versies layout.buildDirectory
    options.generatedSourceOutputDirectory.set(layout.buildDirectory.dir("generated/sources/annotations"))
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        javaParameters.set(true)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

springBoot {
    buildInfo()
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    java {
        palantirJavaFormat(libs.versions.palantir.java.format.get())
        target("src/**/*.java")
    }
    kotlin {
        ktfmt(libs.versions.ktfmt.get())
        target("src/**/*.kt")
    }
}

tasks.bootJar {
    archiveFileName.set("luckystackworker.jar")
    mainClass.set("nl.wilcokas.luckystackworker.LuckystackWorkerApplication")
}