# syntax=docker/dockerfile:1.7

FROM gradle:8.14.5-jdk21 AS build
WORKDIR /workspace

COPY settings.gradle build.gradle gradle.properties ./
COPY libs ./libs
COPY gateway ./gateway

RUN --mount=type=cache,target=/root/.gradle/caches,sharing=locked \
    --mount=type=cache,target=/root/.gradle/wrapper,sharing=locked \
    gradle :gateway:api-gateway:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN groupadd --gid 10001 app &&     useradd --uid 10001 --gid app --shell /usr/sbin/nologin --no-create-home app

WORKDIR /app
COPY --from=build /workspace/gateway/api-gateway/build/libs/api-gateway.jar /app/app.jar

USER 10001:10001

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5   CMD wget -qO- http://127.0.0.1:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java",   "-XX:+UseContainerSupport",   "-XX:MaxRAMPercentage=75.0",   "-Djava.security.egd=file:/dev/./urandom",   "-jar", "/app/app.jar"]
