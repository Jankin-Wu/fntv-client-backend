# fntv-desktop-backend
**飞牛影视桌面端的后端项目**

## 项目简介
作为飞牛影视桌面端的后端服务，提供对视频的转码支持，使用 FFmpeg 对视频进行转码，支持HLS协议返回视频流，目前仅支持原画播放，后期会支持画质切换。

## 使用说明
### 克隆项目
```shell
git clone https://github.com/Jankin-Wu/fntv-desktop-backend.git
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
如果使用gpu编解码：
```shell
docker build -f Dockerfile_hwaccel -t fntv-desktop-backend:latest .
```
#### 运行
##### 使用 CPU 软解（对 CPU 性能有要求）
```shell
# -v 需要挂载存储视频文件的文件目录，容器挂载路径需要和宿主机路径保持一致，根据实际需求可挂载多个目录
docker run -d \
--restart=always \
--name fntv-desktop-backend \
-p 8080:8080 \
-v /vol2/1000/video:/vol2/1000/video \
fntv-desktop-backend:latest
```

##### 使用核显转码
```shell
# -v 需要挂载存储视频文件的文件目录，容器挂载路径需要和宿主机路径保持一致，根据实际需求可挂载多个目录
# --device /dev/dri:/dev/dri 为挂载核显驱动
docker run -d \
--restart=always \
--name fntv-desktop-backend \
--device /dev/dri:/dev/dri \
-p 8080:8080 \
-v /vol2/1000/video:/vol2/1000/video \
fntv-desktop-backend:latest
```
##### 使用 Nvidia 显卡转码
1. 在宿主机安装 NVIDIA Container Toolkit<br>
**参考:**</br>
[安装 NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/latest/install-guide.html)<br>
[Linux 使用 CUDA Docker 镜像加速视频转码](https://www.cnblogs.com/myzony/p/18270956/linux-cuda-docker-video-transcoding)

2. 启动容器
```shell
# -v 需要挂载存储视频文件的文件目录，容器挂载路径需要和宿主机路径保持一致，根据实际需求可挂载多个目录
docker run -d \
--restart=always \
--name fntv-desktop-backend \
--gpus all \
--runtime=nvidia \
-e NVIDIA_DRIVER_CAPABILITIES=all \
--device /dev/dri:/dev/dri \
-p 8080:8080 \
-v /vol2/1000/video:/vol2/1000/video \
fntv-desktop-backend:latest
```
### 本地部署

```shell
# 如果nas里已经装了 FFmpeg，则跳过此步骤
sudo apt update && sudo apt install -y ffmpeg
```

```shell
java -jar build/libs/fntv-desktop-backend-*.jar --spring.profiles.active=release
```
## API

| URL                            | 请求方法 | 参数说明                                                   | 接口说明                                          |
|--------------------------------|------|--------------------------------------------------------|-----------------------------------------------|
| /v/media/{mediaGuid}/{fileName} | GET  | mediaGuid：对应飞牛影视要播放的视频的guid<br> fileName: m38u或ts的文件名称 | 提供给播放器使用的 HLS 协议接口                            |
| /v/media/info/save          | POST | 保存视频信息，具体见代码中的 MediaInfoSaveRequest                    | 在播放前需要调用这个接口将视频信息传递给后端，用于后续视频转码               |
| /v/media/play/info         | POST  | 播放参数，具体见代码中的 PlayRequest                                              | 在播放前或修改播放参数后需要调用这个接口将播放参数传递给后端，返回 HLS 协议的 URL |

