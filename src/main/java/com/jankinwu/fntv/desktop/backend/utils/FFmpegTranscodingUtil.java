package com.jankinwu.fntv.desktop.backend.utils;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.PipeOutput;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.jankinwu.fntv.desktop.backend.enums.HlsFileEnum;

import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class FFmpegTranscodingUtil {

    public static void sliceMediaToTs(String ffmpegPath, String inputVideoPath, OutputStream outputStream,
                                      boolean enableHardwareEncoding, String tsFileName, Integer duration, Integer fps) {
        Path ffmpegBin = ffmpegPath == null ? null : Paths.get(ffmpegPath);

        int tsNum = parseFileNumberFromTsName(tsFileName);
        long StartTime = (long) tsNum * duration;
        UrlInput urlInput = UrlInput.fromUrl(inputVideoPath);
        if (enableHardwareEncoding) {
            urlInput.addArguments("-hwaccel", "auto");
        }
        urlInput.setPosition(StartTime)
                .setDuration(duration)
                .addArgument("-copyts");
        PipeOutput pipeOutput = PipeOutput.pumpTo(outputStream)
                .setFormat("mpegts")
                .addArguments("-sc_threshold", "0")
                .addArguments("-force_key_frames", "expr:gte(t,n_forced*2)")
                .addArguments("-q:v", "0")
                .addArguments("-b:v", "0");
        if (Objects.nonNull(fps)) {
            pipeOutput.addArguments("-vf", "fps=fps=" + fps)
                    .addArguments("-g", fps.toString())
                    .addArguments("-keyint_min", fps.toString());
        }
        FFmpeg ffmpeg = FFmpeg.atPath(ffmpegBin)
                .addInput(urlInput)
                .addOutput(pipeOutput);
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
