package com.jankinwu.fntv.client.service.impl;

import com.jankinwu.fntv.client.service.BuildIndexTaskService;
import com.jankinwu.fntv.client.config.AppConfig;
import com.jankinwu.fntv.client.repository.FnMediaInfoRepository;
import com.jankinwu.fntv.client.repository.domain.FnMediaInfoDO;
import com.jankinwu.fntv.client.utils.M3u8Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingIndexTaskServiceImpl implements BuildIndexTaskService {

    private final FnMediaInfoRepository fnMediaInfoRepository;

    private final AppConfig appConfig;

    @Override
    public void saveOrUpdateMediaInfo(String mediaGuid, String mediaName, String mediaFullPath, String mediaType,
                                      String mediaFormat, Long duration, String category, Integer fps) {
        FnMediaInfoDO mediaInfo = fnMediaInfoRepository.getByMediaGuid(mediaGuid);
        String m3u8Content = M3u8Util.generateM3u8Content(BigDecimal.valueOf(duration), BigDecimal.valueOf(appConfig.getSegmentDuration()).divide(BigDecimal.valueOf(1000),2, RoundingMode.HALF_UP));
        if (Objects.nonNull(mediaInfo)) {
            mediaInfo
                    .setMediaName(mediaName)
                    .setMediaFullPath(mediaFullPath)
                    .setMediaDuration(duration)
                    .setMediaFormat(mediaFormat)
                    .setCategory(category)
                    .setAvgFrameRate(fps)
                    .setM3u8Content(m3u8Content);
            fnMediaInfoRepository.updateById(mediaInfo);
            return;
        }
//        String hlsTempDir = System.getProperty("java.io.tmpdir") + "/hls_" + System.currentTimeMillis();
//        String m3u8Content = null;
//        try {
//            m3u8Content = FFmpegUtil.generateM3u8Content(appConfig.getFfmpegPath(), appConfig.getSegmentDuration(), mediaFullPath, hlsTempDir);
//        } catch (IOException e) {
//            log.error("generate m3u8 content error", e);
//        }
//        Map<String, Long> keyFrameToSegmentsMap = FFmpegUtil.matchKeyFrameToSegments(appConfig.getFfmpegPath(), mediaFullPath, m3u8Content);
        mediaInfo = FnMediaInfoDO.builder()
                .mediaGuid(mediaGuid)
                .mediaName(mediaName)
                .mediaFullPath(mediaFullPath)
                .mediaType(mediaType)
                .mediaFormat(mediaFormat)
                .mediaDuration(duration)
                .category(category)
                .m3u8Content(m3u8Content)
                .avgFrameRate(fps)
//                .tsStartTimeMap(JSONObject.toJSONString(keyFrameToSegmentsMap))
                .build();
        fnMediaInfoRepository.save(mediaInfo);
    }
}
