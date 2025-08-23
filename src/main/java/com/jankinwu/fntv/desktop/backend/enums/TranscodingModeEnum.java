package com.jankinwu.fntv.desktop.backend.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TranscodingModeEnum {

    HW_ONLY("hw_only"),
    SW_ONLY("sw_only"),
    HW_SW_SWITCH("hw_sw_switch");

    private final String code;

    public static TranscodingModeEnum getByCode(String code) {
        for (TranscodingModeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
