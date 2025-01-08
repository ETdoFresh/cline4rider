plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.0"
}

group = "com.cline"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Remove explicit kotlin-stdlib dependency as it's provided by the platform
    compileOnly(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
}

kotlin {
    jvmToolchain(17)
}

intellij {
    version.set("2024.1.1")
    type.set("RD")
    downloadSources.set(true)
    updateSinceUntilBuild.set(true)
    // No additional plugins needed for basic functionality
    plugins.set(listOf())
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
    
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("242.*")
    }

    runIde {
        autoReloadPlugins.set(true)
        jvmArgs("-XX:+UseG1GC")
        jvmArgs("-XX:MaxMetaspaceSize=512m")
        jvmArgs("-XX:ReservedCodeCacheSize=512m")
        jvmArgs("-Xms128m")
        jvmArgs("-Xmx2048m")
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
            apiVersion = "1.8"
            languageVersion = "1.8"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    instrumentCode {
        enabled = false
    }
}
