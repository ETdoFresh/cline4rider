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
        url = uri("https://www.jetbrains.com/intellij-repository/releases")
    }
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
    
    // Test dependencies
    testImplementation("junit:junit:4.13.2") {
        because("JUnit 4 support for legacy tests")
    }
    implementation("com.jetbrains:ideaIC:2024.3") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        because("IntelliJ Platform test framework")
    }
    testImplementation("com.jetbrains.intellij.idea:ideaIC:2024.3") {
        because("IntelliJ Platform test framework")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.21") {
        because("Kotlin/JUnit integration")
    }
    testImplementation("org.junit.vintage:junit-vintage-engine:5.9.3") {
        because("Bridge between JUnit 4 and JUnit Platform")
    }
    
    // Resolve stdlib conflict
    compileOnly(kotlin("stdlib-jdk8"))
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
