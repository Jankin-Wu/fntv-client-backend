package com.jankinwu.fntvdesktopbackend.controller;

import com.jankinwu.fntvdesktopbackend.dto.req.VideoToHlsReq;
import com.jankinwu.fntvdesktopbackend.service.VideoExchangeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Jankin-Wu
 * @description 视频转换
 * @date 2025-08-18 11:02
 **/
@RestController
@RequiredArgsConstructor
@RequestMapping("/video/exchange")
public class VideoExchangeController {

    private final VideoExchangeService videoExchangeService;

    @GetMapping("/get/hls")
    public void getHls(@RequestParam("videoPath") String videoPath, HttpServletResponse response) {
        try {
            // 检查视频文件是否存在
            File videoFile = new File(videoPath);
            if (!videoFile.exists()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//                response.getWriter().write("Video file not found");
                return;
            }

            // 设置响应头
            response.setContentType("application/vnd.apple.mpegurl");
            response.setHeader("Content-Disposition", "inline; filename=\"playlist.m3u8\"");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");

            // 调用服务进行转换并返回流
            videoExchangeService.convertToHls(videoPath, response.getOutputStream());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//                response.getWriter().write("Error converting video: " + e.getMessage());
        }
    }
}
