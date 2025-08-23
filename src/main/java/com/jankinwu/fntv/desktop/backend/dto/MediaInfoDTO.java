package com.jankinwu.fntv.desktop.backend.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Jankin-Wu
 * @description 媒体信息缓存实体
 * @date 2025-08-21 11:23
 **/
@Data
@Builder
@Accessors(chain = true)
public class MediaInfoDTO {

    /**
     * 媒体名称
     */
    private String mediaName;

    /**
     * 媒体全路径
     */
    private String mediaFullPath;

    /**
     * 媒体GUID
     */
    private String mediaGuid;

    /**
     * 媒体类型
     */
    private String mediaType;

    /**
     * 媒体格式
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
     * ts文件起始时间map
     */
    private String tsStartTimeMap;

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

    /**
     * 编码格式
     */
    private String codecName;
}
