import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij") version "1.17.2"
    kotlin("jvm") version "1.9.21"
}

group = "com.rooveterinary"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://plugins.jetbrains.com/maven")
    }
}

intellij {
    version = "2024.3"
    type = "IC"
    plugins = listOf("com.intellij.java", "com.intellij.modules.json")
    downloadSources = true
    updateSinceUntilBuild = true
    instrumentCode = false // Temporarily disable instrumentation
}

dependencies {
    // Kotlin stdlib is automatically added by the Kotlin plugin
}

tasks {
    patchPluginXml {
        sinceBuild.set("243")
        untilBuild.set("243.*")
    }

    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    runIde {
        autoReloadPlugins.set(true)
        jvmArgs("-Xmx2048m")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
