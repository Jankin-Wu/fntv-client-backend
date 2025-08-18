package com.jankinwu.fntvdesktopbackend.utils;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author 皖刚
 * @description ffmpeg 工具类
 * @date 2025-08-18 13:57
 **/
public class FFmpegUtil {

    public static void convertToHls(String videoPath, String hlsPath) throws FFmpegFrameGrabber.Exception, FFmpegFrameRecorder.Exception {
        avutil.av_log_set_level(avutil.AV_LOG_INFO);
        FFmpegLogCallback.set();

        Path hlsDir = Paths.get(hlsPath).getParent();
        if (hlsDir != null) {
            try {
                Files.createDirectories(hlsDir);
            } catch (Exception e) {
                System.err.println("无法创建HLS目录: " + hlsDir.toString());
                throw new RuntimeException("无法创建HLS输出目录: " + e.getMessage(), e);
            }
        }

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath);
        grabber.start();

        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(hlsPath, grabber.getImageWidth(), grabber.getImageHeight());
        recorder.setFormat("hls");
        recorder.setVideoBitrate(grabber.getVideoBitrate());
        recorder.setFrameRate(grabber.getVideoFrameRate());
        recorder.setGopSize((int) grabber.getVideoFrameRate() * 2);
        recorder.setOption("hls_time", "10");
        recorder.setOption("hls_list_size", "0");
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);

        if (grabber.getAudioChannels() > 0) {
            recorder.setAudioChannels(grabber.getAudioChannels());
            recorder.setAudioBitrate(grabber.getAudioBitrate());
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        }

        recorder.start();

        Frame frame;
        try {
            while ((frame = grabber.grab()) != null) {
                recorder.record(frame);
            }
        } finally {
            recorder.stop();
            grabber.stop();
        }

        System.out.println("视频已成功转换为m3u8格式！");
    }
}
