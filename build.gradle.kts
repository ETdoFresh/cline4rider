plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.0"
}

group = "com.cline"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.1.1")
    type.set("RD")
    downloadSources.set(true)
    plugins.set(listOf())
}

tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("242.*")
    }
    
    buildSearchableOptions {
        enabled = false
    }
}
