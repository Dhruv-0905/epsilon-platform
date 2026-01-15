#Build stage
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN nvm clean package -DskipTests

#Run stage
FROM openjdk:17.0.1-jdk-slim
COPY --from=build /target/epsilon-platform-1.0.0.jar epsilon.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "epsilon.jar"]