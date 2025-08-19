package com.jankinwu.fntv.desktop.backend.dto.req;

import lombok.Data;

/**
 * @author Jankin-Wu
 * @description 视频转HLS协议请求
 * @date 2025-08-18 11:12
 **/
@Data
public class PlayRequest {

    private String videoPath;

    private String mediaGuid;
}
