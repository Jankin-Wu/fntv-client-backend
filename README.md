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
docker compose build
```
如果使用 nvidia 显卡编解码：
1. 在宿主机安装 NVIDIA Container Toolkit<br>
   **参考:**</br>
   [安装 NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/latest/install-guide.html)<br>
   [Linux 使用 CUDA Docker 镜像加速视频转码](https://www.cnblogs.com/myzony/p/18270956/linux-cuda-docker-video-transcoding)
2. 构建镜像
```shell
docker compose -f docker-compose.nvidia.yml build
```
#### 运行

```shell
docker-compose up -d
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

