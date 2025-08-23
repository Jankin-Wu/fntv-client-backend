package com.jankinwu.fntv.desktop.backend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CodecApiEnum {

    VAAPI("vaapi"),
    QSV("qsv"),
    NVENC("nvenc"),
    SW("sw");
    private final String code;
    ;
}
