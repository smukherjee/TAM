FROM eclipse-temurin:21 AS build
WORKDIR /app
# Install Maven
RUN apt-get update && apt-get install -y maven
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
