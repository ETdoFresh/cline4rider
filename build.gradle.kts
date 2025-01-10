plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.intellij") version "1.17.0"
}

group = "com.cline"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.20")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.20")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.JETBRAINS)
    }
}

intellij {
    version.set("2024.1.1")
    type.set("RD")
    instrumentCode.set(false)  // Disable code instrumentation to avoid JDK path issues
    downloadSources.set(true)
    updateSinceUntilBuild.set(false)
    sandboxDir.set(project.layout.buildDirectory.dir("idea-sandbox").get().asFile.absolutePath)
}

tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("241.*")
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
            showStackTraces = true
        }
        systemProperties(
            "idea.home.path" to "${project.buildDir}/idea-sandbox",
            "idea.force.use.core.classloader" to "true",
            "idea.use.core.classloader.for.plugin.path" to "true",
            "idea.is.internal" to "true",
            "idea.plugins.path" to "${project.buildDir}/idea-sandbox/plugins",
            "idea.test.environment" to "true"
        )
        environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
        maxHeapSize = "2g"
        
        doFirst {
            project.delete(project.buildDir.resolve("idea-sandbox"))
            project.mkdir(project.buildDir.resolve("idea-sandbox"))
        }
    }

    buildSearchableOptions {
        enabled = false
    }

    prepareSandbox {
        doLast {
            copy {
                from("src/test/testData")
                into("${buildDir}/idea-sandbox/plugins/${project.name}/testData")
            }
        }
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjvm-default=all")
        }
    }
}
