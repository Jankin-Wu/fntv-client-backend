package com.jankinwu.fntv.client.dto;

import lombok.Data;

import java.util.List;

/**
 * 设备信息
 */
@Data
public class DeviceInfoDTO {

    private String osName;

    private List<String> hardwareModule;

    /**
     * 首选硬件加速技术
     */
    private String preferredHwAccelApi;
}
