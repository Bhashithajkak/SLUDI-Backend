FROM gradle:9.0.0-jdk21 AS build

WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies

COPY src ./src
RUN ./gradlew clean build -x test

FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y openssl && apt-get clean

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 5000

ENTRYPOINT ["java", "-jar", "app.jar"]