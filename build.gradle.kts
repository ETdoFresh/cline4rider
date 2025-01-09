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

sourceSets {
    test {
        resources {
            srcDirs("src/test/resources", "src/test/testData")
        }
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.intellij:platform-test-framework:241.14494.326")
    testImplementation("com.intellij:rider-test-framework:241.14494.326")
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
        systemProperty("idea.home.path", project.layout.buildDirectory.dir("idea-sandbox").get().asFile.absolutePath)
        systemProperty("idea.force.use.core.classloader", "true")
        systemProperty("idea.use.core.classloader.for.plugin.path", "true")
        jvmArgs("-ea", "-Xmx1024m")
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }

    register<JavaExec>("runTests") {
        group = "verification"
        description = "Run tests with IntelliJ Platform test runner"
        
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.cline.ClineTestRunner")
        
        systemProperty("idea.home.path", project.layout.buildDirectory.dir("idea-sandbox").get().asFile.absolutePath)
        systemProperty("idea.force.use.core.classloader", "true")
        systemProperty("idea.use.core.classloader.for.plugin.path", "true")
        
        jvmArgs("-ea", "-Xmx1024m")
    }
}
