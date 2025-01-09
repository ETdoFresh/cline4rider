plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.0"
}

group = "com.cline"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
}

intellij {
    version.set("2024.1")
    type.set("RD")
    plugins.set(emptyList())
    instrumentCode.set(false)  // Disable code instrumentation to avoid JDK path issues
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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

    prepareSandbox {
        enabled = true
        doFirst {
            delete(destinationDir)
        }
    }

    jar {
        enabled = true
    }
}

// Configure Gradle build
dependencies {
    // Gson for JSON handling
    implementation("com.google.code.gson:gson:2.10.1")
}

configurations.all {
    resolutionStrategy {
        // Cache dynamic versions for 10 minutes
        cacheDynamicVersionsFor(10, "minutes")
        // Cache changing modules for 10 minutes
        cacheChangingModulesFor(10, "minutes")
        // Force specific versions if needed
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
        force("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    }
}
