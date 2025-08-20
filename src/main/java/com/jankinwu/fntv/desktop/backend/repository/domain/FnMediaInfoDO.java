package com.jankinwu.fntv.desktop.backend.repository.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 飞牛媒体信息表
 * @TableName FN_MEDIA_INFO
 */
@EqualsAndHashCode(callSuper = true)
@Builder
@TableName(value ="FN_MEDIA_INFO")
@Data
public class FnMediaInfoDO extends BaseDomain{
    /**
     * 主键
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 媒体名称
     */
    @TableField(value = "MEDIA_NAME")
    private String mediaName;

    /**
     * 媒体全路径
     */
    @TableField(value = "MEDIA_FULL_PATH")
    private String mediaFullPath;

    /**
     * 媒体GUID
     */
    @TableField(value = "MEDIA_GUID")
    private String mediaGuid;

    /**
     * 媒体类型
     */
    @TableField(value = "MEDIA_TYPE")
    private String mediaType;

    /**
     * 媒体格式
     */
    @TableField(value = "MEDIA_FORMAT")
    private String mediaFormat;

    /**
     * 媒体时长
     */
    @TableField(value = "DURATION")
    private Long duration;

    /**
     * 媒体分类
     */
    @TableField(value = "CATEGORY")
    private String category;

    /**
     * m3u8文件内容
     */
    @TableField(value = "M3U8_CONTENT")
    private String m3u8Content;

    /**
     * ts文件起始时间map
     */
    @TableField(value = "TS_START_TIME_MAP")
    private String tsStartTimeMap;

    /**
     * 颜色范围类型
     */
    @TableField(value = "COLOR_RANGE_TYPE")
    private String colorRangeType;

    /**
     * 码率
     */
    @TableField(value = "BPS")
    private Long bps;

    /**
     * 平均帧率
     */
    @TableField(value = "AVG_FRAME_RATE")
    private Integer avgFrameRate;
}