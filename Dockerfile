FROM openjdk:17-jdk-slim

LABEL authors="JankinWu"

RUN apt update && \
    apt install -y \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/* \

# 更新 sources.list 文件
RUN echo "deb http://deb.debian.org/debian/ bullseye main contrib non-free" > /etc/apt/sources.list && \
    echo "deb-src http://deb.debian.org/debian/ bullseye main contrib non-free" >> /etc/apt/sources.list && \
    apt update

RUN apt install -y libmfx1 libmfx-tools libva-dev libmfx-dev intel-media-va-driver-non-free vainfo
# 设置环境变量
RUN echo "export LIBVA_DRIVER_NAME=iHD" >> /root/.bashrc


# Intel Media SDK 编译
RUN sudo apt install git cmake pkg-config meson libdrm-dev automake libtool && \
    git clone https://github.com/Intel-Media-SDK/MediaSDK msdk && \
    cd msdk

# 更新包列表
RUN apt update

COPY build/libs/*.jar /app/app.jar

VOLUME '/app/logs'

WORKDIR /app

EXPOSE 8080

# 设置时区为 Asia/Shanghai
ENV TZ=Asia/Shanghai

# 设置容器的时区
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV SPRING_PROFILES_ACTIVE="release"

ENTRYPOINT ["java", "-jar", "app.jar"]