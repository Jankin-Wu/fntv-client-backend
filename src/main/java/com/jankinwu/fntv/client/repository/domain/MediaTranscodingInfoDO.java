package com.jankinwu.fntv.client.repository.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 转码信息表
 * @TableName MEDIA_TRANSCODING_INFO
 */
@Builder
@EqualsAndHashCode(callSuper = true)
@TableName(value ="MEDIA_TRANSCODING_INFO")
@Data
public class MediaTranscodingInfoDO extends BaseDomain{
    /**
     * 主键
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 媒体GUID
     */
    @TableField(value = "MEDIA_GUID")
    private String mediaGuid;

    /**
     * 转码模式
     */
    @TableField(value = "transcoding_mode")
    private String transcodingMode;
}