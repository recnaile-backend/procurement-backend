# SonarQube + Trivy Security Pipeline

This repository uses a layered security pipeline. SonarQube and Trivy are complementary and must not be treated as substitutes for one another.

## What each tool does

| Tool | Purpose | Pipeline stage |
|---|---|---|
| SonarQube | Static code quality, bugs, vulnerabilities, security hotspots, code smells and coverage | Pull request / push |
| Trivy filesystem | Dependency, secret and IaC misconfiguration scanning | Pull request / push |
| Trivy container | Container image vulnerability, secret and misconfiguration scanning | Pull request / push |
| Gitleaks | Git history and committed-secret detection | Pull request / push |

## Required GitHub configuration

Create these repository or organization secrets/variables:

### Secrets

- `SONAR_HOST_URL` - URL of the SonarQube Server, for example `https://sonarqube.example.com`
- `SONAR_TOKEN` - SonarQube token with permission to execute analysis for the project
- `GITLEAKS_LICENSE` - required when the organization/account using Gitleaks requires a license

### Variables

- `SONAR_PROJECT_KEY` - default: `procurement-platform-foundation`
- `SONAR_PROJECT_NAME` - default: `Procurement Platform Foundation`

Never commit a SonarQube token, AWS credential, database password or other secret into Git.

## SonarQube setup

1. Create a SonarQube project with the same project key as `SONAR_PROJECT_KEY`.
2. Generate an analysis token.
3. Store the token in GitHub as `SONAR_TOKEN`.
4. Store the server URL as `SONAR_HOST_URL`.
5. Configure the project's Quality Gate.
6. Configure pull-request decoration in SonarQube so GitHub PRs show the analysis result.

The Gradle SonarQube plugin is applied once at the root of this multi-project build. The pipeline runs tests and JaCoCo before the Sonar analysis and waits for the Quality Gate.

## Recommended Quality Gate

Start with these rules for new code:

- No new blocker/critical bugs
- No new critical vulnerabilities
- Security hotspots reviewed
- Coverage on new code >= 80%
- Duplications on new code < 3%
- Reliability rating A
- Security rating A

Tune the gate to the team's baseline rather than failing the entire project because of legacy findings.

## Trivy policy

The pipeline performs two Trivy scans:

1. Filesystem scan for repository dependencies, secrets and infrastructure-as-code misconfiguration.
2. Container scan against the actual gateway image built from the repository Dockerfile.

The workflow fails on `CRITICAL,HIGH` findings and ignores unfixed vulnerabilities. Exceptions should be reviewed and documented rather than bypassing the scan globally.

## CI flow

```text
Pull Request
   |
   +--> PR Validation ---- build + unit tests + Helm lint
   |
   +--> SonarQube -------- SAST + coverage + Quality Gate
   |
   +--> Trivy FS --------- dependencies + secrets + IaC
   |
   +--> Trivy Image ------- container vulnerabilities
   |
   +--> Gitleaks ---------- Git secrets
   |
   v
All required checks PASS
   |
   v
Merge to develop
```

## Local SonarQube analysis

With a local SonarQube Server available on port 9000:

```powershell
$env:SONAR_HOST_URL="http://localhost:9000"
$env:SONAR_TOKEN="<your-token>"
$env:SONAR_PROJECT_KEY="procurement-platform-foundation"
gradle clean test jacocoTestReport sonar
```

For CI, use GitHub secrets rather than local environment files.
