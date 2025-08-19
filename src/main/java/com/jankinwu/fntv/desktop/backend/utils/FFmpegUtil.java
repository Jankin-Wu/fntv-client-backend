package com.jankinwu.fntv.desktop.backend.utils;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.PipeOutput;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Stream;
import com.jankinwu.fntv.desktop.backend.enums.HlsFileEnum;
import jakarta.servlet.ServletOutputStream;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * @author Jankin-Wu
 * @description ffmpeg 工具类
 * @date 2025-08-18 13:57
 **/
@Slf4j
public class FFmpegUtil {

    /**
     * 对视频进行切片并输出为 TS 格式，保留原始码率、帧率以及分辨率
     *
     * @param ffmpegPath     ffmpeg 可执行文件所在目录（若已在系统 PATH 中可直接传入 null）
     * @param inputVideoPath 原视频文件路径
     * @param outputStream   切片后的 TS 文件的输出流
     * @param startTime      切片的起始时间（单位：秒）
     * @param duration       切片时长（单位：秒）
     * @throws RuntimeException 当执行切片命令失败时抛出
     */
    public static void sliceMediaToTs(String ffmpegPath, String inputVideoPath, OutputStream outputStream, Long startTime,
                                      Long duration, boolean enableHardwareEncoding, boolean useGPUAcceleration) {
        // 若 ffmpegPath 不为空，使用指定路径；否则使用环境变量中可执行文件
        Path ffmpegBin = ffmpegPath == null ? null : Paths.get(ffmpegPath);

//        // 1) 获取源视频的视频编码格式（如 h264 / hevc）
//        String srcCodec = probeVideoCodec(ffmpegBin, inputVideoPath); // 可能返回 null
//        // 2) 若启用硬件编码，且源编码可匹配的硬件编码器存在，则返回该硬件编码器名，否则返回 null
//        String matchedHwEncoder = null;
//        if (enableHardwareEncoding && srcCodec != null) {
//            matchedHwEncoder = detectMatchedHardwareEncoder(ffmpegBin, srcCodec);
//        }

        try {
            FFmpeg ffmpeg = FFmpeg.atPath(ffmpegBin)
                    .addInput(UrlInput.fromPath(Paths.get(inputVideoPath))
                            .setPosition(startTime)
                            .setDuration(duration));

            PipeOutput output = PipeOutput.pumpTo(outputStream)
                    .setFormat("mpegts");
            // 这版只做流拷贝，不做转码
            output.setCodec(StreamType.VIDEO, "copy");
            output.setCodec(StreamType.AUDIO, "copy");

//            if (enableHardwareEncoding && matchedHwEncoder != null) {
//                // 可用的硬件编码器与源编码一致 -> 使用硬件编码器
//                addHwAccelArgs(ffmpeg, matchedHwEncoder, useGPUAcceleration);
//                output.setCodec(StreamType.VIDEO, matchedHwEncoder);
//                output.setCodec(StreamType.AUDIO, "copy");
//            } else {
//                // 未启用硬件编码或不匹配 -> 走 copy（软解/直拷）
//                output.setCodec(StreamType.VIDEO, "copy");
//                output.setCodec(StreamType.AUDIO, "copy");
//            }

            ffmpeg.addOutput(output).execute();

        } catch (Exception e) {
            log.error("视频切片失败: {}", e.getMessage(), e);
            throw new RuntimeException("视频切片失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用ffmpeg获取视频时长
     *
     * @param ffmpegPath ffmpeg可执行文件所在目录（若已在系统PATH中可直接传入null）
     * @param videoPath 视频文件路径
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

    public static void getTsFile(String mediaFullPath, String fileName, ServletOutputStream outputStream, Integer segmentDuration) {
        try {
            // 从文件名中提取序号，例如从"00004.ts"中提取出4
            String fileNumberStr = fileName.replace(HlsFileEnum.TS.getSuffix(), "");
            int fileNumber = Integer.parseInt(fileNumberStr);

            // 根据序号和切片时长计算起始时间
            // 序号从0开始，所以第n个切片的起始时间 = n * segmentDuration
            long startTime = (long) fileNumber * segmentDuration;

            // 调用sliceMediaToTs方法生成切片
            sliceMediaToTs(
                    null,                    // ffmpegPath，使用系统PATH中的ffmpeg
                    mediaFullPath,           // 输入视频文件路径
                    outputStream,            // 输出流
                    startTime,               // 起始时间
                    segmentDuration.longValue(), // 切片时长
                    false,                   // enableHardwareEncoding
                    false                    // useGPUAcceleration
            );

        } catch (NumberFormatException e) {
            throw new RuntimeException("无效的TS文件名格式: " + fileName, e);
        } catch (Exception e) {
            throw new RuntimeException("生成TS文件失败: " + e.getMessage(), e);
        }
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
