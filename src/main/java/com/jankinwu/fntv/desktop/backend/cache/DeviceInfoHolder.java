package com.jankinwu.fntv.desktop.backend.cache;


import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSONObject;
import com.jankinwu.fntv.desktop.backend.config.AppConfig;
import com.jankinwu.fntv.desktop.backend.dto.CodecDTO;
import com.jankinwu.fntv.desktop.backend.dto.DeviceInfoDTO;
import com.jankinwu.fntv.desktop.backend.enums.HwAccelApiEnum;
import com.jankinwu.fntv.desktop.backend.enums.VedioCodingEnum;
import com.jankinwu.fntv.desktop.backend.utils.FFmpegTranscodingUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * 设备信息持有者
 */
@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class DeviceInfoHolder implements ApplicationRunner {

    private final AppConfig appConfig;

    private final DeviceInfoDTO deviceInfo = new DeviceInfoDTO();

    @Override
    public void run(ApplicationArguments args) {
        inspectDevice();
    }

    private void inspectDevice() {
        String osName = System.getProperty("os.name").toLowerCase();
        deviceInfo.setOsName(osName);
        Path ffmpegBin = appConfig.getFfmpegPath() == null ? null : Paths.get(appConfig.getFfmpegPath());
        for (VedioCodingEnum value : VedioCodingEnum.values()) {
            Triple<String, String, List<String>> hwCodecTriple = FFmpegTranscodingUtil.detectMatchedHardwareCodec(ffmpegBin, value.getCode());
            Pair<String, String> softwareCodecPair = FFmpegTranscodingUtil.detectMatchedSoftwareCodec(ffmpegBin, value.getCode());
            if (Objects.isNull(hwCodecTriple)) {
                log.warn("未找到硬件编解码器: {}", value.getCode());
                break;
            }
            if (Objects.isNull(softwareCodecPair)) {
                log.warn("未找到软件编解码器: {}", value.getCode());
                break;
            }
            if (CollUtil.isEmpty(deviceInfo.getHardwareModule())) {
                deviceInfo.setHardwareModule(hwCodecTriple.getRight());
                deviceInfo.setPreferredHwAccelApi(unifiedHwAccelApiName(deviceInfo.getHardwareModule().get(0)));
                log.info("当前系统：{}，可用硬件加速模块: {}，首选硬件加速技术: {}", deviceInfo.getOsName(), deviceInfo.getHardwareModule(), deviceInfo.getPreferredHwAccelApi());
            }
            CodecDTO codecDTO = new CodecDTO()
                    .setHwDecoderName(hwCodecTriple.getLeft())
                    .setHwEncoderName(hwCodecTriple.getMiddle())
                    .setSwDecoderName(softwareCodecPair.getLeft())
                    .setSwEncoderName(softwareCodecPair.getRight());
            value.setCodec(codecDTO);
            log.info("获取可用的{}编码器: {}", value.getCode(), JSONObject.toJSONString(codecDTO));
        }
        log.info("--------------编码器加载完毕--------------");
    }

    public CodecDTO getCodec(String srcCodec) {
        VedioCodingEnum vedioCodingEnum = VedioCodingEnum.getByCode(srcCodec);
        assert vedioCodingEnum != null;
        return vedioCodingEnum.getCodec();
    }

    public String getHwAccelApi() {
        return deviceInfo.getPreferredHwAccelApi();
    }

    private String unifiedHwAccelApiName(String hardwareModules) {
        if (Objects.equals(hardwareModules, "nvidia")) {
            return HwAccelApiEnum.CUDA.getName();
        } else if (Objects.equals(hardwareModules, "i915")) {
            return HwAccelApiEnum.QSV.getName();
        } else if (Objects.equals(hardwareModules, "amdgpu") || Objects.equals(hardwareModules, "radeon")) {
            return HwAccelApiEnum.VAAPI.getName();
        }
        return null;
    }
}
