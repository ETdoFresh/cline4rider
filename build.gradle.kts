plugins {
    kotlin("jvm") version "1.8.22"  // Align with Rider 2024.1 Kotlin version
    id("org.jetbrains.intellij") version "1.17.0"
}

group = "com.cline"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://cache-redirector.jetbrains.com/maven-central") }
    maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
}

intellij {
    version.set("2024.1.1")
    type.set("RD")
    downloadSources.set(true)
    plugins.set(listOf())
    instrumentCode.set(false)
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
    
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("242.*")
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
            apiVersion = "1.8"
            languageVersion = "1.8"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    compileJava {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    test {
        useJUnitPlatform()
    }
}
