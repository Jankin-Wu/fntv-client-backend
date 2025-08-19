package com.jankinwu.fntv.desktop.backend.controller;

import com.jankinwu.fntv.desktop.backend.dto.req.PlayRequest;
import com.jankinwu.fntv.desktop.backend.enums.HlsFileEnum;
import com.jankinwu.fntv.desktop.backend.service.MediaService;
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

    private final MediaService mediaService;

    @GetMapping("/media/{mediaCode}/{fileName}")
    public void getHlsFile(@PathVariable("mediaCode") String mediaCode, @PathVariable("fileName") String fileName, HttpServletResponse response) {
        try {
            if (!fileName.endsWith(HlsFileEnum.M3U8.getSuffix())) {
                response.setContentType("application/vnd.apple.mpegurl");
                mediaService.getM3u8File(mediaCode, response.getOutputStream());
            } else  if (fileName.endsWith(HlsFileEnum.TS.getSuffix())) {
                response.setContentType("video/mp2t");
                mediaService.getTsFile(mediaCode, fileName, response.getOutputStream());
            } else {
                return;
            }
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/api/v1/play/play")
    public void play(@RequestBody PlayRequest request) {

    }

}

