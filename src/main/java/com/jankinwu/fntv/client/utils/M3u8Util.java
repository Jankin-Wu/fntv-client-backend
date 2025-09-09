package com.jankinwu.fntv.client.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.StringJoiner;

/**
 * @author Jankin-Wu
 * @description m3u8文件工具类
 * @date 2025-08-19 14:32
 **/
public class M3u8Util {
    /**
     * 根据视频时长和切片时长生成m3u8文件
     *
     * @param videoDuration 视频总时长（秒）
     * @param segmentDuration 每个切片的时长（秒）
     * @return m3u8文件
     */
    public static File generateM3u8File(BigDecimal videoDuration, BigDecimal segmentDuration) {
        StringJoiner joiner = new StringJoiner("\n");
        // 添加m3u8文件头部信息
        joiner.add("#EXTM3U");
        joiner.add("#EXT-X-VERSION:3");
        joiner.add("#EXT-X-ALLOW-CACHE:NO");
        joiner.add("#EXT-X-TARGETDURATION:" + segmentDuration.setScale(0, RoundingMode.CEILING).intValue());
        joiner.add("#EXT-X-MEDIA-SEQUENCE:0");
        joiner.add("#EXT-X-PLAYLIST-TYPE:VOD");

        // 计算需要的切片数量
        int segmentCount = videoDuration.divide(segmentDuration, RoundingMode.CEILING).intValue();

        // 为每个切片添加信息
        BigDecimal remainingDuration = videoDuration;
        for (int i = 0; i < segmentCount; i++) {
            // 最后一个片段需要特殊处理
            BigDecimal currentSegmentDuration;
            if (i == segmentCount - 1) {
                // 最后一个片段使用剩余的准确时长
                currentSegmentDuration = remainingDuration;
            } else {
                // 其他片段使用标准时长
                currentSegmentDuration = segmentDuration;
            }
            joiner.add("#EXTINF:" + String.format("%.6f", currentSegmentDuration.doubleValue()) + ",");
            joiner.add(String.format("%05d.ts", i));
            remainingDuration = remainingDuration.subtract(currentSegmentDuration);
        }


        // 添加结束标记
        joiner.add("#EXT-X-ENDLIST");

        String content = joiner.toString();

        // 写入文件
        File file = new File("preset.m3u8");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        } catch (IOException e) {
            // 如果写入失败，抛出运行时异常
            throw new RuntimeException("Failed to write m3u8 file", e);
        }

        return file;
    }

    /**
     * 根据视频时长和切片时长生成m3u8文件内容
     *
     * @param videoDuration 视频总时长（秒）
     * @param segmentDuration 每个切片的时长（秒）
     * @return m3u8文件内容字符串
     */
    public static String generateM3u8Content(BigDecimal videoDuration, BigDecimal segmentDuration) {
        StringJoiner joiner = new StringJoiner("\n");
        // 添加m3u8文件头部信息
        joiner.add("#EXTM3U");
        joiner.add("#EXT-X-VERSION:3");
        joiner.add("#EXT-X-ALLOW-CACHE:NO");
        joiner.add("#EXT-X-TARGETDURATION:" + segmentDuration.setScale(0, RoundingMode.CEILING).intValue());
        joiner.add("#EXT-X-MEDIA-SEQUENCE:0");
        joiner.add("#EXT-X-PLAYLIST-TYPE:VOD");

        // 计算需要的切片数量
        int segmentCount = videoDuration.divide(segmentDuration, RoundingMode.CEILING).intValue();

        // 为每个切片添加信息
        BigDecimal remainingDuration = videoDuration;
        for (int i = 0; i < segmentCount; i++) {
            // 最后一个片段需要特殊处理
            BigDecimal currentSegmentDuration;
            if (i == segmentCount - 1) {
                // 最后一个片段使用剩余的准确时长
                currentSegmentDuration = remainingDuration;
            } else {
                // 其他片段使用标准时长
                currentSegmentDuration = segmentDuration;
            }
            joiner.add("#EXTINF:" + String.format("%.6f", currentSegmentDuration.doubleValue()) + ",");
            joiner.add(String.format("%05d.ts", i));
            remainingDuration = remainingDuration.subtract(currentSegmentDuration);
        }

        // 添加结束标记
        joiner.add("#EXT-X-ENDLIST");

        return joiner.toString();
    }
}
