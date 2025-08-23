package com.jankinwu.fntv.desktop.backend.enums;

import com.jankinwu.fntv.desktop.backend.dto.CodecDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 视频编码和编码器映射枚举
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor(force = true)
public enum VedioCodingEnum {

    H264("h264", new CodecDTO()),
    HEVC("hevc", new CodecDTO()),
    AV1("av1", new CodecDTO()),
    ;

    private final String code;

    @Setter
    private CodecDTO codec;

    public static VedioCodingEnum getByCode(String code) {
        for (VedioCodingEnum value : VedioCodingEnum.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

}
