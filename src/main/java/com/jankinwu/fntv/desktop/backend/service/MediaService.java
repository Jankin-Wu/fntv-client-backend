package com.jankinwu.fntv.desktop.backend.service;

import jakarta.servlet.ServletOutputStream;

/**
 * @author Jankin-Wu
 * @description 媒体相关服务
 * @date 2025-08-18 11:09
 **/
public interface MediaService {

    void getM3u8File(String mediaCode, ServletOutputStream outputStream);

    void getTsFile(String mediaCode, String fileName, ServletOutputStream outputStream);
}
