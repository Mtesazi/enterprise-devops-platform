FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -pl services/employee-service -am -DskipTests package spring-boot:repackage

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/employee-service/target/employee-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
