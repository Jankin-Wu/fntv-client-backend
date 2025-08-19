package com.jankinwu.fntv.desktop.backend.dto.resp;

import lombok.Builder;
import lombok.Data;

/**
 * @author Jankin-Wu
 * @description 播放视频响应体
 * @date 2025-08-19 10:25
 **/
@Data
@Builder
public class PlayResponse {

    private String playLink;
}
