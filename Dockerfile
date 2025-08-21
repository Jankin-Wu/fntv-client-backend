FROM openjdk:17-jdk-slim

LABEL authors="JankinWu"

RUN apt-get update && \
    apt-get install -y \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

COPY build/libs/*.jar /app/app.jar

VOLUME '/logs'

WORKDIR /app

EXPOSE 8080

# 设置时区为 Asia/Shanghai
ENV TZ=Asia/Shanghai

# 设置容器的时区
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV SPRING_PROFILES_ACTIVE="release"

ENTRYPOINT ["java", "-jar", "app.jar"]