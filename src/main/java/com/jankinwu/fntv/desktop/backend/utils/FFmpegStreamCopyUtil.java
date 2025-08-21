package com.jankinwu.fntv.desktop.backend.utils;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.PipeOutput;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Stream;
import com.jankinwu.fntv.desktop.backend.enums.HlsFileEnum;
import jakarta.servlet.ServletOutputStream;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * @author Jankin-Wu
 * @description ffmpeg 工具类
 * @date 2025-08-18 13:57
 **/
@Slf4j
public class FFmpegStreamCopyUtil {

    /**
     * 对视频进行切片并输出为 TS 格式，保留原始码率、帧率以及分辨率
     *
     * @param ffmpegPath     ffmpeg 可执行文件所在目录（若已在系统 PATH 中可直接传入 null）
     * @param inputVideoPath 原视频文件路径
     * @param outputStream   切片后的 TS 文件的输出流
     * @param m3u8Content    m3u8文件内容，用于获取切片信息
     * @param tsFileName     TS文件名称
     * @param startTime
     * @param endTime
     * @throws RuntimeException 当执行切片命令失败时抛出
     */
    public static void sliceMediaToTs(String ffmpegPath, String inputVideoPath, OutputStream outputStream,
                                      boolean enableHardwareEncoding, boolean useGPUAcceleration, String m3u8Content,
                                      String tsFileName, Long startTime, Long endTime) {
        Path ffmpegBin = ffmpegPath == null ? null : Paths.get(ffmpegPath);

        try {
            // 从m3u8Content中解析出切片时长和序号
            int fileNumber = parseFileNumberFromTsName(tsFileName);
//            long startTime = parseStartTimeFromM3u8(m3u8Content, fileNumber);
            long duration = parseDurationFromM3u8(m3u8Content, fileNumber);

            // 查找最接近的关键帧时间
            long keyFrameStartTime = findNearestKeyFrame(ffmpegPath, inputVideoPath, startTime);
//            long adjustedStartTime = (long) Math.floor(keyFrameTime);
            // 查找结束时间之后的最近关键帧作为切片结束点
            long keyFrameEndTime = findNextKeyFrame(ffmpegPath, inputVideoPath, endTime);
            // 计算调整后的持续时间，确保总时长不变
//            long adjustedDuration = duration + (startTime - keyFrameTime);
            // 计算调整后的持续时间，确保在关键帧结束
            long adjustedDuration = keyFrameEndTime - keyFrameStartTime;
//            long duration = endTime - startTime;
            // 使用Jaffree进行视频切片，从关键帧开始切割
            FFmpeg ffmpeg = FFmpeg.atPath(ffmpegBin)
                    .addInput(UrlInput.fromUrl(inputVideoPath)
                            .setPosition(keyFrameStartTime)
                            .setDuration(adjustedDuration))
                    .addOutput(PipeOutput.pumpTo(outputStream)
                            .setFormat("mpegts")
                            .addArguments("-g", "24")
                            .copyAllCodecs()
                    );

            // 执行FFmpeg命令并将输出写入流
            ffmpeg.execute();

        } catch (Exception e) {
            log.error("视频切片失败: {}", e.getMessage(), e);
            throw new RuntimeException("视频切片失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用FFprobe查找指定时间点之后的下一个关键帧时间戳
     *
     * @param ffmpegPath ffmpeg可执行文件所在目录
     * @param videoPath  视频文件路径
     * @param targetTime 目标时间（毫秒）
     * @return 目标时间之后的最近关键帧时间戳
     */
    private static long findNextKeyFrame(String ffmpegPath, String videoPath, long targetTime) throws IOException, InterruptedException {
        // 构建ffprobe的完整路径
        String ffprobePath;
        if (ffmpegPath == null) {
            ffprobePath = "ffprobe"; // 使用系统PATH中的ffprobe
        } else {
            Path ffmpegBinPath = Paths.get(ffmpegPath);
            // 检查路径是否指向可执行文件或目录
            if (ffmpegBinPath.toFile().isDirectory()) {
                // 如果是目录，添加ffprobe可执行文件名
                ffprobePath = ffmpegBinPath.resolve("ffprobe.exe").toString();
            } else {
                // 如果是可执行文件路径，替换为ffprobe
                String parentPath = ffmpegBinPath.getParent().toString();
                ffprobePath = Paths.get(parentPath, "ffprobe.exe").toString();
            }
        }

        // 构建FFprobe命令获取关键帧列表
        ProcessBuilder pb = new ProcessBuilder(
                ffprobePath,
                "-select_streams", "v",      // 只处理视频流
                "-skip_frame", "nokey",      // 只返回关键帧
                "-show_entries", "frame=pts_time",
                "-of", "csv=print_section=0",
                videoPath);

        // 启动进程并获取输出
        Process process = pb.start();
        List<Long> keyFrames = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    double ptsInSeconds = Double.parseDouble(line.trim());
                    // 转换为毫秒
                    long ptsInMillis = Math.round(ptsInSeconds * 1000);
                    keyFrames.add(ptsInMillis);
                } catch (NumberFormatException e) {
                    // 忽略格式错误的行
                }
            }
        }

        // 等待进程结束
        process.waitFor(1, TimeUnit.SECONDS);

        if (keyFrames.isEmpty()) {
            return targetTime; // 如果没有找到关键帧，返回目标时间
        }

        // 查找目标时间之后的最近关键帧
        for (Long keyFrame : keyFrames) {
            if (keyFrame >= targetTime) {
                return keyFrame;
            }
        }

        // 如果没有找到之后的关键帧，返回最后一个关键帧
        return keyFrames.get(keyFrames.size() - 1);
    }

    /**
     * 使用FFprobe查找指定时间点附近的关键帧时间戳
     *
     * @param ffmpegPath ffmpeg可执行文件所在目录
     * @param videoPath  视频文件路径
     * @param targetTime 目标时间（毫秒）
     * @return 最接近且不大于目标时间的关键帧时间戳
     */
    private static long findNearestKeyFrame(String ffmpegPath, String videoPath, long targetTime) throws IOException, InterruptedException {
        // 构建ffprobe的完整路径
        String ffprobePath;
        if (ffmpegPath == null) {
            ffprobePath = "ffprobe"; // 使用系统PATH中的ffprobe
        } else {
            Path ffmpegBinPath = Paths.get(ffmpegPath);
            // 检查路径是否指向可执行文件或目录
            if (ffmpegBinPath.toFile().isDirectory()) {
                // 如果是目录，添加ffprobe可执行文件名
                ffprobePath = ffmpegBinPath.resolve("ffprobe.exe").toString();
            } else {
                // 如果是可执行文件路径，替换为ffprobe
                String parentPath = ffmpegBinPath.getParent().toString();
                ffprobePath = Paths.get(parentPath, "ffprobe.exe").toString();
            }
        }


        // 构建FFprobe命令获取关键帧列表
        ProcessBuilder pb = new ProcessBuilder(
                ffprobePath,
                "-select_streams", "v",      // 只处理视频流
                "-skip_frame", "nokey",      // 只返回关键帧
                "-show_entries", "frame=pts_time",
                "-of", "csv=print_section=0",
                "-read_intervals", "%+#1",   // 性能优化: 只读取必要部分
                videoPath);

        // 启动进程并获取输出
        Process process = pb.start();
        List<Long> keyFrames = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    double ptsInSeconds = Double.parseDouble(line.trim());
                    // 转换为毫秒
                    long ptsInMillis = Math.round(ptsInSeconds * 1000);
                    keyFrames.add(ptsInMillis);
                } catch (NumberFormatException e) {
                    // 忽略格式错误的行
                }
            }
        }

        // 等待进程结束
        process.waitFor(1, TimeUnit.SECONDS);

        if (keyFrames.isEmpty()) {
            return 0; // 如果没有找到关键帧，返回0
        }

        // 查找最接近的关键帧
        return findClosest(keyFrames, targetTime);
    }

    /**
     * 在有序列表中找到最接近目标值的元素
     */
    private static long findClosest(List<Long> list, long target) {
        int left = 0;
        int right = list.size() - 1;
        int mid = 0;

        // 二分查找
        while (left <= right) {
            mid = left + (right - left) / 2;
            long midVal = list.get(mid);

            if (midVal < target) {
                left = mid + 1;
            } else if (midVal > target) {
                right = mid - 1;
            } else {
                return midVal; // 精确匹配
            }
        }

        // 确定最接近的值
        if (left >= list.size()) {
            return list.get(right);
        } else if (right < 0) {
            return list.get(left);
        } else {
            long leftDiff = Math.abs(list.get(left) - target);
            long rightDiff = Math.abs(list.get(right) - target);
            return leftDiff < rightDiff ? list.get(left) : list.get(right);
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

    /**
     * 从m3u8内容中解析指定序号切片的开始时间
     *
     * @param m3u8Content m3u8文件内容
     * @param fileNumber  文件序号
     * @return 开始时间（毫秒）
     */
    private static long parseStartTimeFromM3u8(String m3u8Content, int fileNumber) {
        // 解析m3u8文件，计算第fileNumber个切片的开始时间
        String[] lines = m3u8Content.split("\n");
        long startTime = 0;

        int currentSegment = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#EXTINF:")) {
                if (currentSegment == fileNumber) {
                    return startTime;
                }
                // 提取时长信息
                String durationStr = line.substring(8, line.length() - 1); // 去掉"#EXTINF:"和逗号
                int duration = (int) Math.ceil(Double.parseDouble(durationStr) * 1000);
                startTime += duration;
                currentSegment++;
            }
        }

        // 如果没有找到对应序号的切片，返回计算得出的时间
        return startTime;
    }

    /**
     * 从m3u8内容中解析指定序号切片的时长
     *
     * @param m3u8Content m3u8文件内容
     * @param fileNumber  文件序号
     * @return 切片时长（毫秒）
     */
    private static long parseDurationFromM3u8(String m3u8Content, int fileNumber) {
        String[] lines = m3u8Content.split("\n");

        int currentSegment = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#EXTINF:")) {
                if (currentSegment == fileNumber) {
                    // 提取时长信息
                    String durationStr = line.substring(8, line.length() - 1); // 去掉"#EXTINF:"和逗号
                    return (long) Math.ceil(Double.parseDouble(durationStr) * 1000);
                }
                currentSegment++;
            }
        }

        // 如果没有找到，默认返回10秒
        return 10;
    }

    /**
     * 使用ffmpeg获取视频时长
     *
     * @param ffmpegPath ffmpeg可执行文件所在目录（若已在系统PATH中可直接传入null）
     * @param videoPath  视频文件路径
     * @return 视频时长（单位秒），四舍五入保留六位小数
     */
    public static BigDecimal getVideoDuration(String ffmpegPath, String videoPath) {
        Path ffmpegBin = ffmpegPath == null ? null : Paths.get(ffmpegPath);

        try {
            FFprobeResult result = FFprobe.atPath(ffmpegBin)
                    .setInput(videoPath)
                    .setShowStreams(false)
                    .setShowFormat(true)
                    .execute();

            if (result.getFormat() != null && result.getFormat().getDuration() != null) {
                // 使用BigDecimal进行四舍五入保留六位小数
                BigDecimal duration = new BigDecimal(result.getFormat().getDuration());
                return duration.setScale(6, RoundingMode.HALF_UP);
            }

            // 如果从format中获取不到，尝试从视频流中获取
            if (result.getStreams() != null) {
                for (Stream stream : result.getStreams()) {
                    if (stream.getCodecType() == StreamType.VIDEO && stream.getDuration() != null) {
                        BigDecimal duration = new BigDecimal(stream.getDuration());
                        return duration.setScale(6, RoundingMode.HALF_UP);
                    }
                }
            }

            throw new RuntimeException("无法获取视频时长");

        } catch (Exception e) {
            throw new RuntimeException("获取视频时长失败: " + e.getMessage(), e);
        }
    }

    public static void getTsFile(String mediaFullPath, String fileName, ServletOutputStream outputStream,
                                 Integer segmentDuration, String ffmpegPath, String m3u8Content, Long startTime, Long endTime) {
        try {
            // 从文件名中提取序号，例如从"00004.ts"中提取出4
//            String fileNumberStr = fileName.replace(HlsFileEnum.TS.getSuffix(), "");
//            int fileNumber = Integer.parseInt(fileNumberStr);
//
//            // 根据序号和切片时长计算起始时间
//            // 序号从0开始，所以第n个切片的起始时间 = n * segmentDuration
//            long startTime = (long) fileNumber * segmentDuration;

            // 调用sliceMediaToTs方法生成切片
            sliceMediaToTs(
                    ffmpegPath,                    // ffmpegPath，使用系统PATH中的ffmpeg
                    mediaFullPath,           // 输入视频文件路径
                    outputStream,            // 输出流
                    false,                   // enableHardwareEncoding
                    false,                   // useGPUAcceleration
                    m3u8Content,
                    fileName,
                    startTime,
                    endTime
            );

        } catch (NumberFormatException e) {
            throw new RuntimeException("无效的TS文件名格式: " + fileName, e);
        } catch (Exception e) {
            throw new RuntimeException("生成TS文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用FFprobe获取视频全部关键帧列表
     *
     * @param ffmpegPath ffmpeg可执行文件所在目录
     * @param videoPath  视频文件路径
     * @return 最接近且不大于目标时间的关键帧时间戳
     */
    private static List<Long> getAllKeyFrameTimestamps(String ffmpegPath, String videoPath) throws IOException, InterruptedException {
        // 构建ffprobe的完整路径
        String ffprobePath;
        if (ffmpegPath == null) {
            ffprobePath = "ffprobe"; // 使用系统PATH中的ffprobe
        } else {
            Path ffmpegBinPath = Paths.get(ffmpegPath);
            // 检查路径是否指向可执行文件或目录
            if (ffmpegBinPath.toFile().isDirectory()) {
                // 如果是目录，添加ffprobe可执行文件名
                ffprobePath = ffmpegBinPath.resolve("ffprobe.exe").toString();
            } else {
                // 如果是可执行文件路径，替换为ffprobe
                String parentPath = ffmpegBinPath.getParent().toString();
                ffprobePath = Paths.get(parentPath, "ffprobe.exe").toString();
            }
        }


        // 构建FFprobe命令获取关键帧列表
        ProcessBuilder pb = new ProcessBuilder(
                ffprobePath,
                "-select_streams", "v",      // 只处理视频流
                "-skip_frame", "nokey",      // 只返回关键帧
                "-show_entries", "frame=pts_time",
                "-of", "csv=print_section=0",
                videoPath);

        // 启动进程并获取输出
        Process process = pb.start();
        List<Long> keyFrames = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    double ptsInSeconds = Double.parseDouble(line.trim());
                    // 转换为毫秒
                    long ptsInMillis = Math.round(ptsInSeconds * 1000);
                    keyFrames.add(ptsInMillis);
                } catch (NumberFormatException e) {
                    // 忽略格式错误的行
                }
            }
        }

        // 等待进程结束
        process.waitFor(1, TimeUnit.SECONDS);

        return keyFrames;
    }

    /**
     * 解析m3u8文件内容中的切片开始时间
     *
     * @param m3u8Content m3u8文件内容
     * @return Map，key为ts文件名称，value为对应切片的开始时间（毫秒）
     */
    public static Map<String, Long> parseSegmentStartTimesFromM3u8(String m3u8Content) {
        Map<String, Long> segmentStartTimes = new LinkedHashMap<>();
        String[] lines = m3u8Content.split("\n");

        long startTime = 0;
        int fileNumber = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#EXTINF:")) {
                // 生成ts文件名，格式为5位数字，例如"00000.ts"
                String tsFileName = String.format("%05d.ts", fileNumber);

                // 将当前切片的开始时间与文件名关联
                segmentStartTimes.put(tsFileName, startTime);

                // 提取时长信息
                String durationStr = line.substring(8, line.length() - 1); // 去掉"#EXTINF:"和逗号
                long duration = (long) Math.ceil(Double.parseDouble(durationStr) * 1000);

                // 更新下一个切片的开始时间
                startTime += duration;
                fileNumber++;
            }
        }

        return segmentStartTimes;
    }

    /**
     * 构造一个map，key为ts文件名称，value为最接近该ts开始时间的关键帧时间
     *
     * @param ffmpegPath ffmpeg可执行文件所在目录
     * @param videoPath  视频文件路径
     * @param m3u8Content m3u8文件内容
     * @return Map，key为ts文件名称，value为最接近该ts开始时间的关键帧时间（毫秒）
     */
    public static Map<String, Long> matchKeyFrameToSegments(String ffmpegPath, String videoPath, String m3u8Content) {
        try {
            // 获取所有关键帧时间戳
            List<Long> keyFrames = getAllKeyFrameTimestamps(ffmpegPath, videoPath);

            // 获取所有ts文件名及其开始时间
            Map<String, Long> segmentStartTimes = parseSegmentStartTimesFromM3u8(m3u8Content);

            // 构造结果map
            Map<String, Long> result = new LinkedHashMap<>();

            // 为每个ts文件找到最接近的关键帧时间
            for (Map.Entry<String, Long> entry : segmentStartTimes.entrySet()) {
                String tsFileName = entry.getKey();
                Long segmentStartTime = entry.getValue();

                // 找到最接近的关键帧时间
                Long closestKeyFrameTime = findClosest(keyFrames, segmentStartTime);
                result.put(tsFileName, closestKeyFrameTime);
            }

            return result;
        } catch (Exception e) {
            log.error("匹配关键帧与切片失败: {}", e.getMessage(), e);
            throw new RuntimeException("匹配关键帧与切片失败: " + e.getMessage(), e);
        }
    }

    /**
     *
     * @param ffmpegPath ffmpeg可执行文件所在目录
     * @param segmentDuration 切片时长（毫秒）
     * @param inputVideoPath 输入视频路径
     * @param tempDir 临时目录
     * @return
     * @throws IOException
     */
    public static String generateM3u8Content(String ffmpegPath, Integer segmentDuration, String inputVideoPath, String tempDir) throws IOException {
        Path ffmpegBin = ffmpegPath == null ? null : Paths.get(ffmpegPath);

        // 确保输入参数是有效的
        if (segmentDuration == null || inputVideoPath == null || inputVideoPath.isEmpty()) {
            throw new IllegalArgumentException("Invalid input parameters.");
        }

        Path tempDirPath = Paths.get(tempDir);
        // 检查临时目录是否存在，如果不存在则创建
        if (!Files.exists(tempDirPath)) {
            Files.createDirectories(tempDirPath);
        }
        // 设置输出文件路径
        Path m3u8FilePath = tempDirPath.resolve("output.m3u8");

        // 配置 FFmpeg 命令
        FFmpeg.atPath(ffmpegBin)
                .addInput(UrlInput.fromPath(Paths.get(inputVideoPath)))
                .addOutput(
                        UrlOutput.toPath(m3u8FilePath)
                                .copyAllCodecs()
                                .setFormat("hls")
//                                .addArguments("-bsf:v", "h264_mp4toannexb")
                                .addArguments("-hls_time", String.valueOf(segmentDuration / 1000))
                                .addArguments("-hls_list_size", "0")
                                .addArguments("-hls_flags", "independent_segments")
                                .addArguments("-hls_segment_filename", tempDirPath.resolve("%05d.ts").toString())
                )
                .execute();

        // 返回 M3U8 文件的内容
        return Files.readString(m3u8FilePath);
    }

//    // 使用 ffprobe 获取源视频的 codec_name（例如：h264、hevc、mpeg2video 等）
//    private static String probeVideoCodec(Path ffmpegBin, String inputVideoPath) {
//        List<String> cmd = new ArrayList<>();
//        cmd.add(ffmpegBin == null ? "ffprobe" : ffmpegBin.resolve("ffprobe").toString());
//        cmd.addAll(Arrays.asList(
//                "-v", "error",
//                "-select_streams", "v:0",
//                "-show_entries", "stream=codec_name",
//                "-of", "default=noprint_wrappers=1:nokey=1",
//                inputVideoPath
//        ));
//        List<String> out = runCommand(cmd);
//        if (!out.isEmpty()) {
//            return out.get(0).trim().toLowerCase();
//        }
//        return null;
//    }
//
//    /**
//     * 仅在“可用硬件编码器与源视频编码一致”时返回硬件编码器名，否则返回 null。
//     * 仅示例常见映射：h264 -> [h264_nvenc, h264_qsv, h264_amf]；hevc -> [hevc_nvenc, hevc_qsv, hevc_amf]
//     * 可按需要扩展（如 vaapi、videotoolbox、av1_* 等）。
//     */
//    private static String detectMatchedHardwareEncoder(Path ffmpegBin, String srcCodec) {
//        // 规范化为我们关注的类别
//        String family = normalizeCodecFamily(srcCodec); // h264 / hevc / other
//        if (family == null) return null;
//
//        // 候选硬件编码器优先级：NVENC > QSV > AMF（可按需调整/扩展）
//        List<String> candidates = new ArrayList<>();
//        switch (family) {
//            case "h264":
//                candidates = Arrays.asList("h264_nvenc", "h264_qsv", "h264_amf");
//                break;
//            case "hevc":
//                candidates = Arrays.asList("hevc_nvenc", "hevc_qsv", "hevc_amf");
//                break;
//            default:
//                return null;
//        }
//
//        // 查询本机 ffmpeg 已启用的编码器
//        List<String> encoders = runFfmpegInfo(ffmpegBin, "-encoders");
//
//        // 从候选中选第一个可用的
//        for (String c : candidates) {
//            boolean exists = encoders.stream().anyMatch(line -> line.contains(c));
//            if (exists) return c;
//        }
//        return null;
//    }
//
//    // 将 codec_name 归一化到我们关心的族
//    private static String normalizeCodecFamily(String codec) {
//        if (codec == null) return null;
//        String c = codec.toLowerCase();
//        if (c.contains("h264") || c.contains("avc")) return "h264";
//        if (c.contains("hevc") || c.contains("h265")) return "hevc";
//        return null; // 其它暂不尝试硬件编码
//    }
//
//    // 根据编码器供应商添加最基础的硬件加速参数（仅在硬编时添加）
//    private static void addHwAccelArgs(FFmpeg ffmpeg, String encoder, boolean useGPUAcceleration) {
//        if (!useGPUAcceleration || encoder == null) return;
//        if (encoder.endsWith("_nvenc")) {
//            ffmpeg.addArguments("-hwaccel", "cuda");
//        } else if (encoder.endsWith("_qsv")) {
//            ffmpeg.addArguments("-hwaccel", "qsv");
//        } else if (encoder.endsWith("_amf")) {
//            // Windows 通常可用 dxva2/d3d11va 作为解码加速。这里以 dxva2 为例。
//            ffmpeg.addArguments("-hwaccel", "dxva2");
//        }
//        // 注：不同平台/驱动可能需要更多参数：-init_hw_device、-filter_hw_device、-vf hwupload 等
//        // 若采用 VAAPI/videotoolbox 需做额外适配，请按环境扩展。
//    }
//
//    private static List<String> runFfmpegInfo(Path ffmpegBin, String arg) {
//        List<String> cmd = new ArrayList<>();
//        cmd.add(ffmpegBin == null ? "ffmpeg" : ffmpegBin.resolve("ffmpeg").toString());
//        cmd.add(arg);
//        return runCommand(cmd);
//    }
//
//    private static List<String> runCommand(List<String> command) {
//        List<String> result = new ArrayList<>();
//        Process process = null;
//        BufferedReader reader = null;
//        try {
//            ProcessBuilder pb = new ProcessBuilder(command);
//            pb.redirectErrorStream(true);
//            process = pb.start();
//            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                result.add(line);
//            }
//            process.waitFor();
//        } catch (IOException | InterruptedException e) {
//            // 可按需记录日志
//        } finally {
//            if (reader != null) {
//                try { reader.close(); } catch (IOException ignored) {}
//            }
//            if (process != null) {
//                process.destroy();
//            }
//        }
//        return result;
//    }

}
