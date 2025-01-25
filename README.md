# AI Helper IntelliJ Plugin

A plugin for IntelliJ IDEA that provides AI assistance capabilities through a convenient sidebar interface.

## Development Setup

### Prerequisites
- Java JDK 17
- IntelliJ IDEA 2023.1 or newer
- Gradle 7.4.2 (via wrapper)

### Building the Plugin
```bash
# Build the plugin
./gradlew build

# Run in a development instance of IntelliJ IDEA
./gradlew runIde
```

### Project Structure
- `src/main/java/com/aihelper/window/` - Main plugin UI components
- `src/main/resources/META-INF/` - Plugin configuration files

## Features
- Sidebar tool window integration
- Home, Tasks, and History tabs
- AI-assisted development tools (coming soon)

## Contributing
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request