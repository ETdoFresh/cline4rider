# Recommended Solution: Using Bundled Kotlin Plugin

## Updated Configuration Files

### build.gradle.kts
```kotlin
plugins {
    id("org.jetbrains.intellij") version "1.17.4"
    kotlin("jvm") version "1.9.21"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
}

// Configure IntelliJ Plugin
intellij {
    version.set("2024.3")
    type.set("IC")
    plugins.set(listOf("java", "Kotlin")) // Use bundled Kotlin plugin
    updateSinceUntilBuild.set(false)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("241.*")
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
```

### plugin.xml
```xml
<idea-plugin>
    <id>com.example.myplugin</id>
    <name>My Kotlin Plugin</name>
    <vendor email="support@example.com" url="https://example.com">Your Name</vendor>
    
    <description><![CDATA[
        A simple IntelliJ plugin written in Kotlin.
    ]]></description>

    <depends>com.intellij.modules.platform</depends>
    <depends>org.jetbrains.kotlin</depends>
    
    <extensions defaultExtensionNs="com.intellij">
        <!-- Add your extensions here -->
    </extensions>

    <actions>
        <action id="MyAction" class="com.example.myplugin.MyAction" text="Say Hello">
            <add-to-group group-id="ToolsMenu" anchor="last"/>
        </action>
    </actions>
</idea-plugin>
```

## Build Commands
```bash
./gradlew clean
./gradlew buildPlugin
./gradlew runIde
```

## Verification Steps
1. Launch the sandboxed IntelliJ IDEA instance
2. Verify the Kotlin plugin is available
3. Check that your plugin appears in the Tools menu
4. Test the "Say Hello" action