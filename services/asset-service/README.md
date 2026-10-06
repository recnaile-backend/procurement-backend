# Asset Service — Phase 14: Testing & OpenAPI

This package contains the Phase 14 Asset Service implementation with testing and OpenAPI documentation.

## Fixes in this package

- Simplified `AssetController` OpenAPI response annotations to avoid compiler/parser failures from nested `@Content`/`@Schema` annotations while preserving documented HTTP response codes and descriptions.
- Corrected all `@ApiResponses` blocks and response separators.
- Fixed `ApiErrorContractTest` to use the Java 21 record reflection API: `Class#getRecordComponents()` plus component-name inspection. `Class#getRecordComponent(String)` does not exist in Java 21.
- Preserved the standardized `ApiError` record contract and existing application behavior.

## Verify locally

```powershell
Remove-Item -Recurse -Force .\services\asset-service
```

Extract this ZIP into:

```text
procurement-backend\services\asset-service
```

Then run:

```powershell
.\gradlew :services:asset-service:clean :services:asset-service:test :services:asset-service:build
```

After a successful build:

```powershell
docker compose build --no-cache asset-service
docker compose up -d asset-service
docker compose ps asset-service
docker compose logs --tail=100 asset-service
curl.exe http://localhost:8089/actuator/health
```

Swagger UI:

```text
http://localhost:8089/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8089/v3/api-docs
```

## Git

```powershell
git add services/asset-service
git commit -m "test(asset): complete testing and openapi"
git status
```

## Important

The package was statically validated for ZIP integrity, Java source structure, OpenAPI annotation structure, and the corrected Java record reflection test. Gradle/Docker execution must be performed in the user's project environment because those tools are not available in the package-building environment.


## JWT validation compatibility

This Asset Service uses the same local/dev JWT validation model as Return Service.

- Default validation mode: `hs256`
- Shared secret: `JWT_SECRET_KEY`
- Issuer: `JWT_ISSUER` (default `auth-service`)
- Minimum secret length: 32 bytes
- `JWT_VALIDATION_MODE=jwk` remains available for asymmetric/JWK deployments.
- `roles` and `role` JWT claims are mapped to `ROLE_*` authorities.

Local/dev environment:

```text
JWT_SECRET_KEY=<same secret used by Auth Service>
JWT_ISSUER=auth-service
JWT_VALIDATION_MODE=hs256
```

RS256/JWK environment:

```text
JWT_VALIDATION_MODE=jwk
AUTH_JWK_SET_URI=http://auth-service:8081/oauth2/jwks
```

Git:

```powershell
git add services/asset-service
git commit -m "fix(asset): align jwt validation"
git status
```
