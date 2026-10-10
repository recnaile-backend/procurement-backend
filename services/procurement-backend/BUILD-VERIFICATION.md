# Build Verification

## Prerequisites

- JDK 21
- Git
- Docker (only if using the local infrastructure)
- Gradle Wrapper included in this repository

## Local build

Windows:

```cmd
gradlew.bat clean test build --no-daemon
```

Linux/macOS:

```bash
./gradlew clean test build --no-daemon
```

## Verify the observability library

```bash
./gradlew :libs:common-observability:clean
./gradlew :libs:common-observability:compileJava
./gradlew :libs:common-observability:test
```

## CI recommendation

GitHub Actions should use the repository Gradle Wrapper (`./gradlew`) rather than a globally installed `gradle` command. This keeps CI on the repository's pinned Gradle version.

## Expected result

A successful build ends with:

```text
BUILD SUCCESSFUL
```

The observability library explicitly imports the Spring Boot 3.5.16 BOM so its Spring Boot and Micrometer dependencies have resolvable versions.
