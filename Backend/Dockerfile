# Build from repo root:
#   docker compose build backend
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY Backend/postfolio /workspace
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
