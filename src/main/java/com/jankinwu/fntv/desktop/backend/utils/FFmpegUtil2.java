package com.jankinwu.fntv.desktop.backend.utils;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.jankinwu.fntv.desktop.backend.enums.HlsFileEnum;
import jakarta.servlet.ServletOutputStream;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * @author Jankin-Wu
 * @description ffmpeg 强制生成关键帧
 * @date 2025-08-20 11:38
 **/
public class FFmpegUtil2 {

    public static void getTsFile(String mediaFullPath, String fileName, ServletOutputStream outputStream, Integer segmentDuration, String ffmpegPath, String m3u8Content) {
        try {
            // 从文件名中提取序号，例如从"00004.ts"中提取出4
            String fileNumberStr = fileName.replace(HlsFileEnum.TS.getSuffix(), "");
            int fileNumber = Integer.parseInt(fileNumberStr);
//
            // 根据序号和切片时长计算起始时间
            // 序号从0开始，所以第n个切片的起始时间 = n * segmentDuration
            long startTime = (long) fileNumber * segmentDuration;

            // 调用sliceMediaToTs方法生成切片
            sliceMediaToTs(
                    ffmpegPath,                    // ffmpegPath，使用系统PATH中的ffmpeg
                    mediaFullPath,           // 输入视频文件路径
                    outputStream,            // 输出流
                    false,                   // enableHardwareEncoding
                    false,                   // useGPUAcceleration
                    m3u8Content,
                    fileName,
                    segmentDuration,
                    startTime
            );

        } catch (NumberFormatException e) {
            throw new RuntimeException("无效的TS文件名格式: " + fileName, e);
        } catch (Exception e) {
            throw new RuntimeException("生成TS文件失败: " + e.getMessage(), e);
        }
    }

    private static void sliceMediaToTs(String ffmpegPath, String mediaFullPath, ServletOutputStream outputStream,
                                       boolean b, boolean b1, String m3u8Content, String fileName, Integer segmentDuration,
                                       long startTime) {
        Path ffmpegBin = ffmpegPath == null ? null : Paths.get(ffmpegPath);
        Path tempTsFile = null;
        try {
            // 创建临时文件用于存储生成的ts片段
            tempTsFile = Files.createTempFile("segment_", ".ts");

            // 使用Jaffree构建FFmpeg命令
            FFmpeg ffmpeg = FFmpeg.atPath(ffmpegBin)
                    .addInput(
                            UrlInput.fromUrl(mediaFullPath)
                                    .setPosition(startTime, TimeUnit.MILLISECONDS)  // 起始时间（秒和毫秒）
                                    .setDuration(segmentDuration, TimeUnit.MILLISECONDS)  // 持续时间（秒和毫秒）
                    )
                    .addOutput(
                            UrlOutput.toUrl(tempTsFile.toString())
                                    .copyAllCodecs()
                                    .addArguments("-hls_playlist_type", "vod")
                                    .addArguments("-force_key_frames", "expr:gte(t,n_forced*2)")
                    );

            // 执行FFmpeg命令
            ffmpeg.execute();

            // 将生成的ts文件写入输出流
            try (InputStream inputStream = Files.newInputStream(tempTsFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }

        } catch (Exception e) {
            throw new RuntimeException("生成TS文件失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            if (tempTsFile != null) {
                try {
                    Files.deleteIfExists(tempTsFile);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
