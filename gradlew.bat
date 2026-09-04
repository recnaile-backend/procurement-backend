@echo off
setlocal
if not exist "%~dp0gradle\wrapper\gradle-wrapper.jar" (
  echo Gradle wrapper JAR is not present in this checkout.
  echo Run: gradle wrapper --gradle-version 8.14.5
  echo Or run: powershell -ExecutionPolicy Bypass -File scripts\bootstrap-windows.ps1
  exit /b 1
)
java -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
