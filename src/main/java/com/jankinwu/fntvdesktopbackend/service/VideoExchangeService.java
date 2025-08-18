package com.jankinwu.fntvdesktopbackend.service;

import jakarta.servlet.ServletOutputStream;

/**
 * @author Jankin-Wu
 * @description 视频转换 service
 * @date 2025-08-18 11:09
 **/
public interface VideoExchangeService {
    void convertToHls(String videoPath, ServletOutputStream outputStream);
}
