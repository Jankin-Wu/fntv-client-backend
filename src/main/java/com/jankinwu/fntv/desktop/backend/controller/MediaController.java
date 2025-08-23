package com.jankinwu.fntv.desktop.backend.controller;

import com.jankinwu.fntv.desktop.backend.dto.req.MediaInfoSaveRequest;
import com.jankinwu.fntv.desktop.backend.dto.req.PlayRequest;
import com.jankinwu.fntv.desktop.backend.dto.resp.PlayResponse;
import com.jankinwu.fntv.desktop.backend.enums.HlsFileEnum;
import com.jankinwu.fntv.desktop.backend.service.MediaService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @author Jankin-Wu
 * @description 视频转换
 * @date 2025-08-18 11:02
 **/
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v/media")
public class MediaController {

    private final MediaService mediaService;

    @GetMapping("/{mediaGuid}/{fileName}")
    public void getHlsFile(@PathVariable("mediaGuid") String mediaGuid, @PathVariable("fileName") String fileName, HttpServletResponse response) {
        try {
            if (fileName.endsWith(HlsFileEnum.M3U8.getSuffix())) {
                response.setContentType("application/vnd.apple.mpegurl");
                mediaService.getM3u8File(mediaGuid, response.getOutputStream());
            } else if (fileName.endsWith(HlsFileEnum.TS.getSuffix())) {
                response.setContentType("video/mp2t");
                mediaService.getTsFile(mediaGuid, fileName, response.getOutputStream());
            } else {
                return;
            }
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
        } catch (Exception e) {
            log.error("Error converting video: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/info/save")
    public Boolean saveMediaInfo(@RequestBody MediaInfoSaveRequest request) {
        mediaService.saveOrUpdateMediaInfo(request);
        return true;
    }

    @PostMapping("/play/info")
    public PlayResponse play(@RequestBody PlayRequest request) {
        mediaService.saveOrUpdatePlayInfo(request);
        return mediaService.getPlayResponse(request.getMediaGuid());
    }
}

