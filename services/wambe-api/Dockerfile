# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY services/wambe-api/.mvn services/wambe-api/.mvn
COPY services/wambe-api/mvnw services/wambe-api/pom.xml services/wambe-api/
COPY docs/sdlc/stories/US-002-create-publish-event/contracts docs/sdlc/stories/US-002-create-publish-event/contracts
COPY services/wambe-api/src services/wambe-api/src

WORKDIR /workspace/services/wambe-api
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw \
    && ./mvnw package -DskipTests -Dmaven.wagon.http.retryHandler.count=5

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 wambe
COPY --from=build /workspace/services/wambe-api/target/wambe-api-*.jar /app/wambe-api.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/wambe-api.jar"]
