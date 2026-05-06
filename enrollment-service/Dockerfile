FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -U -s docker/maven/settings.xml -pl enrollment-service -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN mkdir -p /logs
COPY --from=build /workspace/enrollment-service/target/enrollment-service-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
