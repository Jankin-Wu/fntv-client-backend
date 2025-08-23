package com.jankinwu.fntv.desktop.backend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 视频编码枚举
 */
@Getter
@AllArgsConstructor
public enum VedioCodingEnum {

    H264("h264"),
    HEVC("hevc"),
    AV1("av1"),
    ;

    private final String code;
}
