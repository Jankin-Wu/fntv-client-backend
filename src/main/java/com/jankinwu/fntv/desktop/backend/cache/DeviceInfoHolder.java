package com.jankinwu.fntv.desktop.backend.cache;


import com.alibaba.fastjson2.JSONObject;
import com.jankinwu.fntv.desktop.backend.config.AppConfig;
import com.jankinwu.fntv.desktop.backend.dto.CodecDTO;
import com.jankinwu.fntv.desktop.backend.dto.DeviceInfoDTO;
import com.jankinwu.fntv.desktop.backend.enums.VedioCodingEnum;
import com.jankinwu.fntv.desktop.backend.utils.FFmpegTranscodingUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
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
        Path ffmpegBin = appConfig.getFfmpegPath() == null ? null : Paths.get(appConfig.getFfmpegPath());
        for (VedioCodingEnum value : VedioCodingEnum.values()) {
            Triple<String, String, String> hwCodecPair = FFmpegTranscodingUtil.detectMatchedHardwareCodec(ffmpegBin, value.getCode());
            Pair<String, String> softwareCodecPair = FFmpegTranscodingUtil.detectMatchedSoftwareCodec(ffmpegBin, value.getCode());
            if (Objects.isNull(hwCodecPair)) {
                log.warn("未找到硬件编解码器: {}", value.getCode());
                break;
            }
            if (Objects.isNull(softwareCodecPair)) {
                log.warn("未找到软件编解码器: {}", value.getCode());
                break;
            }
            if (StringUtils.isBlank(deviceInfo.getHwApi())) {
                deviceInfo.setHwApi(hwCodecPair.getRight());
            }
            CodecDTO codecDTO = new CodecDTO()
                    .setHwDecoderName(hwCodecPair.getLeft())
                    .setHwEncoderName(hwCodecPair.getMiddle())
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

    public String getHwApi() {
        return deviceInfo.getHwApi();
    }
}
