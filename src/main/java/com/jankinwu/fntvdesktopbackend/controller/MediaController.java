package com.jankinwu.fntvdesktopbackend.controller;

import com.jankinwu.fntvdesktopbackend.dto.req.PlayRequest;
import com.jankinwu.fntvdesktopbackend.service.VideoExchangeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author Jankin-Wu
 * @description 视频转换
 * @date 2025-08-18 11:02
 **/
@RestController
@RequiredArgsConstructor
@RequestMapping("/v")
public class MediaController {

    private final VideoExchangeService videoExchangeService;

    @GetMapping("/media/{videoCode}/{fileName}")
    public void getHlsFile(@PathVariable("videoCode") String videoCode, @PathVariable("fileName") String fileName, HttpServletResponse response) {
        try {

            // 设置响应头
            if (!fileName.endsWith(".m3u8")) {
                response.setContentType("application/vnd.apple.mpegurl");
//                response.setHeader("Content-Disposition", "inline; filename=\"playlist.m3u8\"");
            } else  if (fileName.endsWith(".ts")) {
                response.setContentType("video/mp2t");
//                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            }
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");



        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//                response.getWriter().write("Error converting video: " + e.getMessage());
        }
    }

    @PostMapping("/api/v1/play/play")
    public void play(@RequestBody PlayRequest request) {

    }

}

