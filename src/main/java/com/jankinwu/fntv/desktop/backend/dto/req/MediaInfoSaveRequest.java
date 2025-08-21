package com.jankinwu.fntv.desktop.backend.dto.req;

import lombok.Data;

/**
 * @author Jankin-Wu
 * @description 视频转HLS协议请求
 * @date 2025-08-18 11:12
 **/
@Data
public class MediaInfoSaveRequest {

    /**
     * 媒体GUID
     */
    private String mediaGuid;

    /**
     * 媒体名称
     */
    private String mediaName;

    /**
     * 媒体全路径
     */
    private String mediaFullPath;

    /**
     * 媒体类型
     */
    private String mediaType;

    /**
     * 媒体包装格式
     */
    private String mediaFormat;

    /**
     * 媒体时长(秒)
     */
    private Long mediaDuration;

    /**
     * 媒体分类
     */
    private String category;

    /**
     * m3u8文件内容
     */
    private String m3u8Content;

    /**
     * 颜色范围类型
     */
    private String colorRangeType;

    /**
     * 码率
     */
    private Long bps;

    /**
     * 平均帧率
     */
    private Integer avgFrameRate;
}
