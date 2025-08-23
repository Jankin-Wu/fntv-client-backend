package com.jankinwu.fntv.desktop.backend.cache;


import com.alibaba.fastjson2.JSONObject;
import com.jankinwu.fntv.desktop.backend.config.AppConfig;
import com.jankinwu.fntv.desktop.backend.enums.VedioCodingEnum;
import com.jankinwu.fntv.desktop.backend.utils.FFmpegTranscodingUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备信息持有者
 */
@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class DeviceInfoHolder {

    private final AppConfig appConfig;

    /**
     * 可用硬件编码器
     * key: 编码名称
     * value: 硬件编码器名称
     */
    private Map<String, Pair<String, String>> availableHwEncoders = new HashMap<>();

    @PostConstruct
    private void inspectDevice() {
        Path ffmpegBin = appConfig.getFfmpegPath() == null ? null : Paths.get(appConfig.getFfmpegPath());
        for (VedioCodingEnum value : VedioCodingEnum.values()) {
            availableHwEncoders.put(value.getCode(),
                    FFmpegTranscodingUtil.detectMatchedHardwareCodec(ffmpegBin, value.getCode()));
        }
        log.info("可用硬件编码器: {}", JSONObject.toJSONString(availableHwEncoders));
    }
}
