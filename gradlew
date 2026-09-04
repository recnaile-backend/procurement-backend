#!/bin/sh
if [ ! -f "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" ]; then
  echo "Gradle wrapper JAR is not present. Run: gradle wrapper --gradle-version 8.14.5" >&2
  exit 1
fi
exec java -classpath "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
