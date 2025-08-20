package com.jankinwu.fntv.desktop.backend.utils;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.PipeOutput;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.jankinwu.fntv.desktop.backend.enums.HlsFileEnum;

import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FFmpegTranscodingUtil {

    public static void sliceMediaToTs(String ffmpegPath, String inputVideoPath, OutputStream outputStream,
                                      boolean enableHardwareEncoding, boolean useGPUAcceleration,
                                      String tsFileName, Integer duration) {
        Path ffmpegBin = ffmpegPath == null ? null : Paths.get(ffmpegPath);

        int tsNum = parseFileNumberFromTsName(tsFileName);
        long StartTime = (long) tsNum * duration;
        FFmpeg ffmpeg = FFmpeg.atPath(ffmpegBin)
                .addInput(UrlInput.fromUrl(inputVideoPath)
                        .addArguments("-hwaccel", "auto")
                        .setPosition(StartTime)
                        .setDuration(duration)
                        .addArgument("-copyts"));
        // 根据硬件加速选项设置编码器
        if (enableHardwareEncoding && useGPUAcceleration) {
            ffmpeg = ffmpeg.addOutput(PipeOutput.pumpTo(outputStream)
                    .setFormat("mpegts")
                    .setCodec(StreamType.VIDEO, "h264_nvenc") // NVIDIA GPU编码
                    .addArguments("-vf", "fps=fps=24")
                    .addArguments("-g", "24")
                    .addArguments("-keyint_min", "24")
                    .addArguments("-sc_threshold", "0")
                    .addArguments("-force_key_frames", "expr:gte(t,n_forced*4)")
                    .addArguments("-q:v", "0") // 保持原始视频质量
                    .addArguments("-b:v", "0") // 自动检测并使用原始码率
            );
        } else if (enableHardwareEncoding) {
            ffmpeg = ffmpeg.addOutput(PipeOutput.pumpTo(outputStream)
                    .setFormat("mpegts")
                    .addArguments("-vf", "fps=fps=24")
                    .addArguments("-g", "24")
                    .addArguments("-keyint_min", "24")
                    .addArguments("-sc_threshold", "0")
                    .addArguments("-force_key_frames", "expr:gte(t,n_forced*4)")
                    .addArguments("-bf", "3")
                    .addArguments("-q:v", "0")
                    .addArguments("-b:v", "0")
            );
        } else {
            ffmpeg = ffmpeg.addOutput(PipeOutput.pumpTo(outputStream)
                    .setFormat("mpegts")
                    .addArguments("-vf", "fps=fps=24")
                    .addArguments("-g", "48")
                    .addArguments("-keyint_min", "48")
                    .addArguments("-sc_threshold", "0")
                    .addArguments("-force_key_frames", "expr:gte(t,n_forced*4)")
                    .setCodec(StreamType.VIDEO, "libx264")
                    .addArguments("-q:v", "0")
                    .addArguments("-b:v", "0")
            );
        }

        // 执行FFmpeg命令并将输出写入流
        ffmpeg.execute();

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
}
