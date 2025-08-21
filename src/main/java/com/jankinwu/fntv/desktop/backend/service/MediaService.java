package com.jankinwu.fntv.desktop.backend.service;

import com.jankinwu.fntv.desktop.backend.dto.req.MediaInfoSaveRequest;
import com.jankinwu.fntv.desktop.backend.dto.resp.PlayResponse;
import jakarta.servlet.ServletOutputStream;

/**
 * @author Jankin-Wu
 * @description 媒体相关服务
 * @date 2025-08-18 11:09
 **/
public interface MediaService {

    void getM3u8File(String mediaGuid, ServletOutputStream outputStream);

    void getTsFile(String mediaGuid, String fileName, ServletOutputStream outputStream);

    void saveOrUpdateMediaInfo(MediaInfoSaveRequest request);

    PlayResponse getPlayResponse(String mediaGuid);
}
