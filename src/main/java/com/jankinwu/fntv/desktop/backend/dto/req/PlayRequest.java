package com.jankinwu.fntv.desktop.backend.dto.req;

import lombok.Data;

/**
 * @author Jankin-Wu
 * @description 播放参数
 * @date 2025-08-21 19:19
 **/
@Data
public class PlayRequest {

    /**
     * 媒体GUID
     */
    private String mediaGuid;

    /**
     * 画质
     */
    private String quality;

    /**
     * 转码模式
     */
    private String transcodingMode;
}
