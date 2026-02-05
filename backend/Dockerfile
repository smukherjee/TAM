FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# 1. Download dependencies (better caching with retry)
COPY pom.xml .
RUN n=0; until [ "$n" -ge 5 ]; do mvn dependency:go-offline -B -Djava.net.preferIPv4Stack=true && break; n=$((n+1)); echo "Retry $n..."; sleep 5; done

# 2. Copy source and build
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true -Djava.net.preferIPv4Stack=true

FROM eclipse-temurin:21
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
