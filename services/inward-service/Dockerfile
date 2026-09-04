# syntax=docker/dockerfile:1.7
FROM gradle:8.14.5-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle gradle.properties ./
COPY libs ./libs
COPY services/inward-service ./services/inward-service
RUN --mount=type=cache,target=/root/.gradle/caches,sharing=locked \
    --mount=type=cache,target=/root/.gradle/wrapper,sharing=locked \
    gradle :services:inward-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --shell /usr/sbin/nologin --no-create-home app
WORKDIR /app
COPY --from=build /workspace/services/inward-service/build/libs/inward-service.jar /app/app.jar
USER 10001:10001
EXPOSE 8085
HEALTHCHECK --interval=20s --timeout=5s --start-period=40s --retries=5 CMD wget -qO- http://127.0.0.1:8085/actuator/health/liveness || exit 1
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]
