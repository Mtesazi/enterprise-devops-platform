FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -pl services/discovery-server -am -DskipTests package spring-boot:repackage

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/discovery-server/target/discovery-server-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
