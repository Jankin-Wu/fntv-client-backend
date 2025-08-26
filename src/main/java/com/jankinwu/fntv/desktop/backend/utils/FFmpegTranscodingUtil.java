package com.jankinwu.fntv.desktop.backend.utils;

import cn.hutool.core.collection.CollUtil;
import com.github.kokorin.jaffree.ffmpeg.ChannelOutput;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.jankinwu.fntv.desktop.backend.dto.CodecDTO;
import com.jankinwu.fntv.desktop.backend.enums.HlsFileEnum;
import com.jankinwu.fntv.desktop.backend.enums.HwAccelApiEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Slf4j
public class FFmpegTranscodingUtil {

    /**
     * 切分视频为ts文件
     *
     * @param ffmpegPath                ffmpeg 二进制文件的路径
     * @param inputVideoPath            源视频文件的路径
     * @param outputStream              输出流
     * @param enableHardwareTranscoding 是否使用硬件编码
     * @param tsFileName                ts 文件名
     * @param duration                  视频片段的持续时间
     * @param fps                       视频片段的帧率
     * @param codec                     编解码器
     * @param colorPrimaries            原始色彩空间
     * @param codecName                 编解码器名称
     * @param hwAccelApi                硬件加速技术
     * @throws IOException 抛出 IOException
     */
    public static void sliceMediaToTs(String ffmpegPath, String inputVideoPath, OutputStream outputStream,
                                      boolean enableHardwareTranscoding, String tsFileName, Integer duration, Integer fps,
                                      CodecDTO codec, String colorPrimaries, String codecName, String hwAccelApi) throws IOException {
        Path ffmpegBin = ffmpegPath == null ? null : Paths.get(ffmpegPath);
        SeekableByteChannel outputChannel = null;
        Path tempOutputPath = null;

        try {
            // 生成带UUID的临时文件路径
            String tempDir = System.getProperty("java.io.tmpdir");
            String uuid = UUID.randomUUID().toString();
            String tempFileName = "ffmpeg_output_" + uuid + ".ts";
            tempOutputPath = Paths.get(tempDir, tempFileName);

            // 确保临时目录存在
            Files.createDirectories(tempOutputPath.getParent());
            outputChannel = Files.newByteChannel(tempOutputPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            // 组装输入和输出参数
            UrlInput urlInput = assembleInputArguments(inputVideoPath, tsFileName, duration, codec, hwAccelApi, enableHardwareTranscoding);
            ChannelOutput channelOutput = assembleOutputArguments(outputChannel, tsFileName, fps, codec, hwAccelApi, enableHardwareTranscoding);

            FFmpeg ffmpeg = FFmpeg.atPath(ffmpegBin)
                    .addInput(urlInput)
                    .addOutput(channelOutput);
            // 执行FFmpeg命令并将输出写入流
            ffmpeg.execute();
            // 将文件内容复制到输出流
            Files.copy(tempOutputPath, outputStream);

        } catch (IOException e) {
            log.error("生成TS文件时发生错误", e);
        } finally {
            if (outputChannel != null) {
                try {
                    outputChannel.close();
                } catch (IOException e) {
                    log.error("关闭输出通道时发生错误", e);
                }
            }
            if (tempOutputPath != null) {
                try {
                    Files.deleteIfExists(tempOutputPath);
                } catch (IOException e) {
                    log.error("删除临时文件时发生错误", e);
                }
            }
        }
    }

    /**
     * 组装FFmpeg输入参数
     *
     * @param inputVideoPath            源视频文件路径
     * @param tsFileName                TS文件名
     * @param duration                  视频片段持续时间
     * @param codec                     编解码器信息
     * @param hwAccelApi                硬件加速API
     * @param enableHardwareTranscoding
     * @return UrlInput对象
     */
    private static UrlInput assembleInputArguments(String inputVideoPath, String tsFileName, Integer duration,
                                                   CodecDTO codec, String hwAccelApi, boolean enableHardwareTranscoding) {
        int tsNum = parseFileNumberFromTsName(tsFileName);
        long startTime = (long) tsNum * duration;
        UrlInput urlInput = UrlInput.fromUrl(inputVideoPath);

        urlInput.setPosition(startTime)
                .setDuration(duration)
                .addArgument("-copyts");

        // 使用硬件加速解码
        if (enableHardwareTranscoding && StringUtils.isNotBlank(codec.getHwDecoderName())) {
            if (Objects.equals(hwAccelApi, HwAccelApiEnum.CUDA.getName())) {
                urlInput.addArguments("-c:v", codec.getHwDecoderName())
                        .addArguments("-hwaccel", "cuda")
                        .addArguments("-hwaccel_output_format", "cuda");

            } else if (Objects.equals(hwAccelApi, HwAccelApiEnum.QSV.getName())) {
                urlInput.addArguments("-c:v", codec.getHwDecoderName())
                        .addArguments("-hwaccel", "qsv");
            }
        } else {
            urlInput.addArguments("-c:v", codec.getSwDecoderName());
        }

        return urlInput;
    }

    /**
     * 组装FFmpeg输出参数
     *
     * @param outputChannel             输出通道
     * @param tsFileName                TS文件名
     * @param fps                       帧率
     * @param codec                     编解码器信息
     * @param hwAccelApi                硬件加速API
     * @param enableHardwareTranscoding
     * @return ChannelOutput对象
     */
    private static ChannelOutput assembleOutputArguments(SeekableByteChannel outputChannel, String tsFileName,
                                                         Integer fps, CodecDTO codec, String hwAccelApi, boolean enableHardwareTranscoding) {
        List<String> videoProcessingFilterList = new ArrayList<>();

        ChannelOutput channelOutput = ChannelOutput
                .toChannel(tsFileName, outputChannel)
                .setFormat("mpegts")
                .addArguments("-force_key_frames", "expr:gte(t,n_forced*2)")
                .addArguments("-b:v", "0");

        // 添加帧率参数，保证关键帧对齐
        if (Objects.nonNull(fps)) {
            channelOutput
                    .addArguments("-g", String.valueOf(fps * 2))
                    .addArguments("-keyint_min", String.valueOf(fps * 2));
            videoProcessingFilterList.add("fps=fps=" + fps);
        }
        // 判断是否使用硬件加速编码
        if (enableHardwareTranscoding && StringUtils.isNotBlank(codec.getHwEncoderName())) {
            channelOutput.addArguments("-c:v", codec.getHwEncoderName());
        } else {
            channelOutput.addArguments("-c:v", codec.getSwEncoderName());
        }
        // 添加vaapi滤镜参数
        if (Objects.equals(hwAccelApi, HwAccelApiEnum.VAAPI.getName())) {
            videoProcessingFilterList.add("format=nv12");
            videoProcessingFilterList.add("hwupload");
        } else if (Objects.equals(hwAccelApi, HwAccelApiEnum.QSV.getName())) {
            // 使用QSV低功耗转码，提升转码效率
            channelOutput.addArguments("-low_power", "1");
        }

        // 组装视频转换滤镜参数
        assembleVideoProcessingFilter(videoProcessingFilterList, channelOutput);

        return channelOutput;
    }

    private static void assembleVideoProcessingFilter(List<String> videoProcessingFilterList, ChannelOutput channelOutput) {
        if (CollUtil.isNotEmpty(videoProcessingFilterList)) {
            String videoProcessingFilter = String.join(",", videoProcessingFilterList);
            channelOutput.addArguments("-vf", videoProcessingFilter);
        }
    }

    /**
     * 从TS文件名中解析出文件序号
     *
     * @param tsFileName TS文件名，如"00001.ts"
     * @return 文件序号
     */
    public static int parseFileNumberFromTsName(String tsFileName) {
        try {
            String fileNumberStr = tsFileName.replace(HlsFileEnum.TS.getSuffix(), "");
            return Integer.parseInt(fileNumberStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("无效的TS文件名格式: " + tsFileName, e);
        }
    }

    // 使用 ffprobe 获取源视频的 codec_name（例如：h264、hevc、mpeg2video 等）
    private static String probeVideoCodec(Path ffmpegBin, String inputVideoPath) {
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegBin == null ? "ffprobe" : ffmpegBin.resolve("ffprobe").toString());
        cmd.addAll(Arrays.asList(
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=codec_name",
                "-of", "default=noprint_wrappers=1:nokey=1",
                inputVideoPath
        ));
        List<String> out = runCommand(cmd);
        if (!out.isEmpty()) {
            return out.get(0).trim().toLowerCase();
        }
        return null;
    }

    /**
     * 通过检查系统内核模块来检测可用的硬件编解码器
     */
    public static Triple<String, String, List<String>> detectMatchedHardwareCodec(Path ffmpegBin, String srcCodec) {
        // 规范化为我们关注的类别
        String family = normalizeCodecFamily(srcCodec); // h264 / hevc / other
        if (family == null) return null;

        // 检查系统中加载的硬件相关内核模块
        List<String> availableModules = getLoadedHardwareModules();

        // 根据检测到的硬件模块确定可用的编码器
        List<String> encoderCandidates = getEncoderCandidates(family, availableModules);
        List<String> decoderCandidates = getDecoderCandidates(family, availableModules);


        // 查询本机 ffmpeg 已启用的编码器
        List<String> encoders = runFfmpegInfo(ffmpegBin, "-encoders");
        List<String> decoders = runFfmpegInfo(ffmpegBin, "-decoders");

        String hwEncoder = "";
        String hwDecoder = "";

        // 从候选中选第一个可用的
        assert encoderCandidates != null;
        for (String c : encoderCandidates) {
            boolean exists = encoders.stream().anyMatch(line -> line.contains(c));
            if (exists) {
                hwEncoder = c;
                break;
            }
        }
        assert decoderCandidates != null;
        for (String c : decoderCandidates) {
            boolean exists = decoders.stream().anyMatch(line -> line.contains(c));
            if (exists) {
                hwDecoder = c;
                break;
            }
        }
        return Triple.of(hwDecoder, hwEncoder, availableModules);
    }

    private static List<String> getDecoderCandidates(String family, List<String> availableModules) {
        List<String> decoderCandidates = new ArrayList<>();
        switch (family) {
            case "h264":
                if (availableModules.contains("nvidia")) {
                    decoderCandidates.add("h264_cuvid");
                }
                if (availableModules.contains("i915")) {
                    decoderCandidates.add("h264_qsv");
                }
                if (availableModules.contains("amdgpu") || availableModules.contains("radeon")) {
                    // Todo: 添加 AMD 硬件解码器
                }
                return decoderCandidates;
            case "hevc":
                if (availableModules.contains("nvidia")) {
                    decoderCandidates.add("hevc_cuvid");
                }
                if (availableModules.contains("i915")) {
                    decoderCandidates.add("hevc_qsv");
                }
                if (availableModules.contains("amdgpu") || availableModules.contains("radeon")) {

                }
                return decoderCandidates;
            case "av1":
                if (availableModules.contains("nvidia")) {
                    decoderCandidates.add("av1_cuvid");
                }
                if (availableModules.contains("i915")) {
                    decoderCandidates.add("av1_qsv");
                }
                if (availableModules.contains("amdgpu") || availableModules.contains("radeon")) {

                }
                return decoderCandidates;
            default:
                return null;
        }
    }

    private static List<String> getEncoderCandidates(String family, List<String> availableModules) {
        List<String> encoderCandidates = new ArrayList<>();
        switch (family) {
            case "h264":
                if (availableModules.contains("nvidia")) {
                    encoderCandidates.add("h264_nvenc");
                }
                if (availableModules.contains("i915")) {
                    encoderCandidates.add("h264_qsv");
                }
                if (availableModules.contains("amdgpu") || availableModules.contains("radeon")) {
                    encoderCandidates.add("h264_amf");
                    encoderCandidates.add("h264_vaapi");
                }
                return encoderCandidates;
            case "hevc":
                if (availableModules.contains("nvidia")) {
                    encoderCandidates.add("hevc_nvenc");
                }
                if (availableModules.contains("i915")) {
                    encoderCandidates.add("hevc_qsv");
                }
                if (availableModules.contains("amdgpu") || availableModules.contains("radeon")) {
                    encoderCandidates.add("hevc_amf");
                    encoderCandidates.add("hevc_vaapi");
                }
                return encoderCandidates;
            case "av1":
                if (availableModules.contains("nvidia")) {
                    encoderCandidates.add("av1_nvenc");
                }
                if (availableModules.contains("i915")) {
                    encoderCandidates.add("av1_qsv");
                }
                if (availableModules.contains("amdgpu") || availableModules.contains("radeon")) {
                    encoderCandidates.add("av1_amf");
                    encoderCandidates.add("av1_vaapi");
                }
                return encoderCandidates;
            default:
                return null;
        }
    }

    /**
     * 获取系统中加载的硬件相关内核模块
     */
    private static List<String> getLoadedHardwareModules() {
        List<String> modules = new ArrayList<>();
        String osName = System.getProperty("os.name").toLowerCase();
        try {
            // 在Linux系统上检查内核模块
            if (osName.contains("linux")) {
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", "lsmod | grep -E 'nvidia|amdgpu|radeon|i915'");
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("nvidia")) {
                        modules.add("nvidia");
                    } else if (line.contains("amdgpu")) {
                        modules.add("amdgpu");
                    } else if (line.contains("radeon")) {
                        modules.add("radeon");
                    } else if (line.contains("i915")) {
                        modules.add("i915");
                    }
                }

                process.waitFor();
            } else if (osName.contains("windows")) {
                // Windows硬件检测逻辑
                // 检查NVIDIA
                if (isNvidiaGpuPresent()) {
                    modules.add("nvidia");
                }
                // 检查Intel
                if (isIntelGpuPresent()) {
                    modules.add("i915");
                }
                // 检查AMD
                if (isAmdGpuPresent()) {
                    modules.add("amdgpu");
                }
            }
        } catch (Exception e) {
            log.warn("Error detecting hardware modules: {}", e.getMessage());
        }

        return modules;
    }

    /**
     * 检查NVIDIA GPU是否存在
     */
    private static boolean isNvidiaGpuPresent() {
        try {
            // 首先尝试使用 wmic
            if (tryWmicCheck("nvidia")) {
                return true;
            }
            // 如果 wmic 不可用，尝试使用 PowerShell
            return tryPowerShellCheck("nvidia");
        } catch (Exception e) {
            // 如果上述方法都失败，记录日志并返回 false
            return false;
        }
    }

    /**
     * 检查Intel GPU是否存在
     */
    private static boolean isIntelGpuPresent() {
        try {
            // 首先尝试使用 wmic
            if (tryWmicCheck("intel")) {
                return true;
            }
            // 如果 wmic 不可用，尝试使用 PowerShell
            return tryPowerShellCheck("intel");
        } catch (Exception e) {
            // 如果上述方法都失败，记录日志并返回 false
            return false;
        }
    }

    /**
     * 检查AMD GPU是否存在
     */
    private static boolean isAmdGpuPresent() {
        try {
            // 首先尝试使用 wmic
            if (tryWmicCheck("amd") || tryWmicCheck("radeon")) {
                return true;
            }
            // 如果 wmic 不可用，尝试使用 PowerShell
            return tryPowerShellCheck("amd") || tryPowerShellCheck("radeon");
        } catch (Exception e) {
            // 如果上述方法都失败，记录日志并返回 false
            return false;
        }
    }

    /**
     * 尝试使用 wmic 检查GPU
     */
    private static boolean tryWmicCheck(String keyword) {
        try {
            ProcessBuilder pb = new ProcessBuilder("wmic", "path", "win32_videocontroller", "get", "name");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains(keyword.toLowerCase())) {
                    return true;
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // wmic 不可用，返回 false
            return false;
        }
        return false;
    }

    /**
     * 尝试使用 PowerShell 检查GPU
     */
    private static boolean tryPowerShellCheck(String keyword) {
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command",
                    "Get-WmiObject -Class Win32_VideoController | Select-Object -ExpandProperty Name");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains(keyword.toLowerCase())) {
                    return true;
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // PowerShell 方法也失败
            return false;
        }
        return false;
    }

    // 将 codec_name 归一化到我们关心的族
    private static String normalizeCodecFamily(String codec) {
        if (codec == null) return null;
        String c = codec.toLowerCase();
        if (c.contains("h264") || c.contains("avc")) return "h264";
        if (c.contains("hevc") || c.contains("h265")) return "hevc";
        if (c.contains("av1")) return "av1";
        return null; // 其它暂不尝试硬件编码
    }

    private static List<String> runFfmpegInfo(Path ffmpegBin, String arg) {
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegBin == null ? "ffmpeg" : ffmpegBin.resolve("ffmpeg").toString());
        cmd.add(arg);
        return runCommand(cmd);
    }

    private static List<String> runCommand(List<String> command) {
        List<String> result = new ArrayList<>();
        Process process = null;
        BufferedReader reader = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            process = pb.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            // 可按需记录日志
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
        return result;
    }

    /**
     * 检测匹配的软件编解码器
     *
     * @param ffmpegBin FFmpeg可执行文件路径
     * @param srcCodec  源视频编码格式
     * @return Pair<解码器, 编码器>，如果未找到则对应位置为空字符串
     */
    public static Pair<String, String> detectMatchedSoftwareCodec(Path ffmpegBin, String srcCodec) {
        // 规范化编码器族
        String family = normalizeCodecFamily(srcCodec);
        if (family == null) return null;

        // 获取FFmpeg支持的编解码器列表
        List<String> encoders = runFfmpegInfo(ffmpegBin, "-encoders");
        List<String> decoders = runFfmpegInfo(ffmpegBin, "-decoders");

        String softwareEncoder = "";
        String softwareDecoder = "";

        // 根据编码器族选择合适的软件编解码器
        switch (family) {
            case "h264":
                // 优先选择libx264，如果没有则选择h264
                if (encoders.stream().anyMatch(line -> line.contains("libx264"))) {
                    softwareEncoder = "libx264";
                } else if (encoders.stream().anyMatch(line -> line.contains("h264"))) {
                    softwareEncoder = "h264";
                }

                // 选择h264解码器
                if (decoders.stream().anyMatch(line -> line.contains("h264"))) {
                    softwareDecoder = "h264";
                }
                break;
            case "hevc":
                // 优先选择libx265，如果没有则选择hevc
                if (encoders.stream().anyMatch(line -> line.contains("libx265"))) {
                    softwareEncoder = "libx265";
                } else if (encoders.stream().anyMatch(line -> line.contains("hevc"))) {
                    softwareEncoder = "hevc";
                }

                // 选择hevc解码器
                if (decoders.stream().anyMatch(line -> line.contains("hevc"))) {
                    softwareDecoder = "hevc";
                }
                break;
            case "av1":
                // 优先选择libaom-av1，如果没有则选择av1
                if (encoders.stream().anyMatch(line -> line.contains("libaom-av1"))) {
                    softwareEncoder = "libaom-av1";
                } else if (encoders.stream().anyMatch(line -> line.contains("av1"))) {
                    softwareEncoder = "av1";
                }

                // 选择av1解码器
                if (decoders.stream().anyMatch(line -> line.contains("av1"))) {
                    softwareDecoder = "av1";
                }
                break;
            default:
                // 对于其他编码格式，直接查找是否有匹配的软件编码器
                for (String line : encoders) {
                    if (line.contains(family) && !line.contains("dev")) {
                        // 提取编码器名称（第二列）
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length > 1) {
                            softwareEncoder = parts[1];
                            break;
                        }
                    }
                }

                // 查找对应的解码器
                for (String line : decoders) {
                    if (line.contains(family) && !line.contains("dev")) {
                        // 提取解码器名称（第二列）
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length > 1) {
                            softwareDecoder = parts[1];
                            break;
                        }
                    }
                }
                break;
        }

        return Pair.of(softwareDecoder, softwareEncoder);

    }
}
