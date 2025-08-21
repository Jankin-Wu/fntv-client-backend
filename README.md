# fntv-desktop-backend
**飞牛桌面端的后端项目**

## 项目简介
作为飞牛客户端的后端服务，提供对视频的转码支持，使用 FFmpeg 对视频进行转码，支持HLS协议返回视频流，目前仅支持原画播放，后期会支持画质切换。

## 使用说明
### 克隆项目
```shell
https://github.com/Jankin-Wu/fntv-desktop-backend.git
```
### 构建 Jar 包
```shell
# 跳转到项目目录
cd fntv-desktop-backend

# 清理构建目录
./gradlew clean

# 打包并跳过测试
./gradlew bootJar -x test
```
### Docker 部署

#### 构建 docker 镜像

```shell
docker build -t fntv-desktop-backend:latest .
```
#### 运行
```shell
# -v 需要挂载存储视频文件的文件目录，容器挂载路径需要和宿主机路径保持一致，根据实际需求可挂载多个目录
docker run -d \
--restart=always \
--name fntv-desktop-backend \
-p 8080:8080 \
-v /vol2/1000/video:/vol2/1000/video
fntv-desktop-backend:latest
```
### 本地部署

```shell
# 如果nas里已经装了 FFmpeg，则跳过此步骤
sudo apt update && sudo apt install -y ffmpeg
```

```shell
java -jar build/libs/fntv-desktop-backend-*.jar
```
## API

| URL                             | 请求方法 | 参数说明                                                    | 接口说明                                          |
|---------------------------------|------|---------------------------------------------------------|-----------------------------------------------|
| /v/media/{mediaGuid}/{fileName} | GET  | mediaGuid：对应飞牛影视要播放的视频的guid<br/> fileName: m38u或ts的文件名称 | 提供给播放器使用的HLS协议接口                              |
| /v/api/v1/play                    | POST | 视频参数，具体见代码中的 PlayRequest                                | 在播放前需要调用这个接口将视频信息传递过来，用于后续视频转码，并返回 HLS 协议的URL |


