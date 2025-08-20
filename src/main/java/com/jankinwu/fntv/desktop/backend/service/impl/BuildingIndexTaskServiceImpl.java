package com.jankinwu.fntv.desktop.backend.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.jankinwu.fntv.desktop.backend.config.AppConfig;
import com.jankinwu.fntv.desktop.backend.repository.FnMediaInfoRepository;
import com.jankinwu.fntv.desktop.backend.repository.domain.FnMediaInfoDO;
import com.jankinwu.fntv.desktop.backend.service.BuildIndexTaskService;
import com.jankinwu.fntv.desktop.backend.utils.FFmpegUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingIndexTaskServiceImpl implements BuildIndexTaskService {

    private final FnMediaInfoRepository fnMediaInfoRepository;

    private final AppConfig appConfig;

    @Override
    public void saveMediaInfo(String mediaGuid, String mediaName, String mediaFullPath, String mediaType, String mediaFormat, Long duration, String category) {
        FnMediaInfoDO mediaInfo = fnMediaInfoRepository.getByMediaGuid(mediaGuid);
        if (Objects.nonNull(mediaInfo)) {
            return;
        }
        String hlsTempDir = System.getProperty("java.io.tmpdir") + "/hls_" + System.currentTimeMillis();
//        String m3u8Content = M3u8Util.generateM3u8Content(BigDecimal.valueOf(duration), BigDecimal.valueOf(appConfig.getSegmentDuration()));
        String m3u8Content = null;
        try {
            m3u8Content = FFmpegUtil.generateM3u8Content(appConfig.getFfmpegPath(), appConfig.getSegmentDuration(), mediaFullPath, hlsTempDir);
        } catch (IOException e) {
            log.error("generate m3u8 content error", e);
        }
        Map<String, Long> keyFrameToSegmentsMap = FFmpegUtil.matchKeyFrameToSegments(appConfig.getFfmpegPath(), mediaFullPath, m3u8Content);
        mediaInfo = FnMediaInfoDO.builder()
                .mediaGuid(mediaGuid)
                .mediaName(mediaName)
                .mediaFullPath(mediaFullPath)
                .mediaType(mediaType)
                .mediaFormat(mediaFormat)
                .duration(duration)
                .category(category)
                .m3u8Content(m3u8Content)
                .tsStartTimeMap(JSONObject.toJSONString(keyFrameToSegmentsMap))
                .build();
        fnMediaInfoRepository.save(mediaInfo);
    }
}
