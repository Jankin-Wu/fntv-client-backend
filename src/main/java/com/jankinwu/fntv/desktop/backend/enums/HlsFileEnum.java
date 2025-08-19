package com.jankinwu.fntv.desktop.backend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HlsFileEnum {

    TS("ts", ".ts"),
    M3U8("m3u8", ".m3u8");

    private final String code;
    private final String suffix;
}
