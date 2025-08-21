FROM openjdk:17-jdk-slim

LABEL authors="JankinWu"

RUN apt-get update && \
    apt-get install -y \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

COPY build/libs/*.jar app.jar

VOLUME '/logs'

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE="release"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]