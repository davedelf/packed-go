FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /workspace/app

COPY api-gateway/mvnw .
COPY api-gateway/.mvn .mvn
COPY api-gateway/pom.xml .
COPY api-gateway/src src

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
ARG JAR_FILE=/workspace/app/target/*.jar
COPY --from=build ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
