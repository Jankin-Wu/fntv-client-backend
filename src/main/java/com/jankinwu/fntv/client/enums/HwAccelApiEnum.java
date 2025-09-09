package com.jankinwu.fntv.client.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HwAccelApiEnum {

    CUDA("cuda"),
    QSV("qsv"),
    VAAPI("vaapi"),
    DXVA2("dxva2"),
    D3D11VA("d3d11va"),
    VULKAN("vulkan"),
    OPENCL("opencl"),
    ;
    private final String name;
    ;
}
