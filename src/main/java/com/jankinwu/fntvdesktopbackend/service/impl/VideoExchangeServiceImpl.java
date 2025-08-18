package com.jankinwu.fntvdesktopbackend.service.impl;

import com.jankinwu.fntvdesktopbackend.service.VideoExchangeService;
import com.jankinwu.fntvdesktopbackend.utils.FFmpegUtil;
import jakarta.servlet.ServletOutputStream;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 * @author Jankin-Wu
 * @description 视频转换实现
 * @date 2025-08-18 11:10
 **/
@Service
public class VideoExchangeServiceImpl implements VideoExchangeService {
    @Override
    public void convertToHls(String videoPath, ServletOutputStream outputStream) {
        String tempDir = System.getProperty("java.io.tmpdir") + "/hls/hls_" + System.currentTimeMillis();
        String playlistPath = tempDir + "/playlist.m3u8";

        try {
            Files.createDirectories(Paths.get(tempDir));

//            FFmpegUtil.convertToHls(videoPath, playlistPath);

            File playlistFile = new File(playlistPath);
            int retryCount = 0;
            while (!playlistFile.exists() && retryCount < 100) {
                Thread.sleep(100);
                retryCount++;
            }

            if (!playlistFile.exists()) {
                throw new RuntimeException("FFmpeg未能生成播放列表文件");
            }

            // 读取播放列表内容，并逐行处理路径替换
            StringBuilder modifiedPlaylist = new StringBuilder();
            Files.lines(playlistFile.toPath()).forEach(line -> {
                // 如果是.ts文件行，则替换为完整URL
                if (line.endsWith(".ts")) {
                    String segmentName = line;
                    modifiedPlaylist.append("/video/exchange/get/ts/")
                            .append(segmentName)
                            .append("?tempDir=")
                            .append(tempDir.replace("\\", "/"))
                            .append("\n");
                } else {
                    modifiedPlaylist.append(line).append("\n");
                }
            });

            outputStream.write(modifiedPlaylist.toString().getBytes());
            outputStream.flush();

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // 可以考虑延迟删除临时目录，给客户端一些时间获取片段
            // 或者实现一个定时清理机制
        }
    }


    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
