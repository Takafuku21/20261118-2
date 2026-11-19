FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY demo/ ./
RUN chmod +x ./mvnw && ./mvnw -B clean package

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java", "-Dserver.port=10000", "-jar", "app.jar"]
