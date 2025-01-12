  plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.4"
    kotlin("plugin.serialization") version "1.9.22"
  }

group = "com.etdofresh"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.jgoodies:jgoodies-forms:1.9.0")
}

kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "1.9"
        }
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  version.set("2024.1.7")
  type.set("IC") // Target IDE Platform
  instrumentCode.set(true)

  plugins.set(listOf(
    "com.intellij.platform.images", // Required for AllIcons
    "com.intellij.java", // Required for Java-related icons
    "terminal" // Required for terminal functionality
  ))
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
  }

  patchPluginXml {
    sinceBuild.set("241")
    untilBuild.set("243.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}
