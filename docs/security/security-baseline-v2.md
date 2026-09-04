# Security Baseline v2

## Purpose

This baseline updates the foundation dependency stack to address the vulnerable versions reported by the container scan while preserving Java 21 and Gradle 8.x.

## Baseline

- Java: 21
- Gradle: 8.14.5
- Spring Boot: 3.5.16
- Spring Cloud: 2025.0.3
- Spring Cloud Gateway: managed by the Spring Cloud 2025.0.3 BOM
- Micrometer, Netty, Jackson and Spring Security: managed by Spring Boot 3.5.16
- OpenTelemetry: managed by Spring Boot where supported
- Trivy: CRITICAL/HIGH blocking, unfixed findings ignored
- Gitleaks: full Git-history scan
- SonarQube: runs when `SONAR_HOST_URL` and `SONAR_TOKEN` repository secrets are configured

## SonarQube

Configure these repository secrets before requiring SonarQube as a protected-branch check:

- `SONAR_HOST_URL`
- `SONAR_TOKEN`

Optional repository variables:

- `SONAR_PROJECT_KEY`
- `SONAR_PROJECT_NAME`

If the Sonar secrets are not configured, the workflow skips SonarQube and continues to enforce Trivy and Gitleaks. This avoids a false pipeline failure caused only by missing external Sonar configuration.

## Upgrade policy

Dependabot is configured to keep patch/minor updates flowing while major Spring Boot, Spring Cloud and Gradle wrapper upgrades remain intentional engineering changes. The next planned major migration is Spring Boot 4.x / Spring Cloud 2025.1.x and should be handled as a dedicated compatibility project.
