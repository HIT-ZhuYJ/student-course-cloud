FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -U -s docker/maven/settings.xml -pl teacher-service -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN mkdir -p /logs
COPY --from=build /workspace/teacher-service/target/teacher-service-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
