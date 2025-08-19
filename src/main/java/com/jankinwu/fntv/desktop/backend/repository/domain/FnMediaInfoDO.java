package com.jankinwu.fntv.desktop.backend.repository.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 飞牛媒体信息表
 * @TableName FN_MEDIA_INFO
 */
@TableName(value ="FN_MEDIA_INFO")
@Data
public class FnMediaInfoDO {
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
     * 创建人
     */
    @TableField(value = "CREATE_BY")
    private String createBy;

    /**
     * 修改人
     */
    @TableField(value = "UPDATE_BY")
    private String updateBy;

    /**
     * 创建时间
     */
    @TableField(value = "CREATE_TIME")
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @TableField(value = "UPDATE_TIME")
    private LocalDateTime updateTime;
}