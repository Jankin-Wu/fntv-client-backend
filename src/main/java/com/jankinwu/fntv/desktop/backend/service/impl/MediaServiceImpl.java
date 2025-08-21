package com.jankinwu.fntv.desktop.backend.service.impl;

import com.jankinwu.fntv.desktop.backend.assembler.MediaInfoAssembler;
import com.jankinwu.fntv.desktop.backend.cache.MediaInfoCache;
import com.jankinwu.fntv.desktop.backend.config.AppConfig;
import com.jankinwu.fntv.desktop.backend.dto.MediaInfoDTO;
import com.jankinwu.fntv.desktop.backend.dto.req.MediaInfoSaveRequest;
import com.jankinwu.fntv.desktop.backend.dto.resp.PlayResponse;
import com.jankinwu.fntv.desktop.backend.repository.FnMediaInfoRepository;
import com.jankinwu.fntv.desktop.backend.repository.domain.FnMediaInfoDO;
import com.jankinwu.fntv.desktop.backend.service.MediaService;
import com.jankinwu.fntv.desktop.backend.utils.FFmpegTranscodingUtil;
import com.jankinwu.fntv.desktop.backend.utils.M3u8Util;
import jakarta.servlet.ServletOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author Jankin-Wu
 * @description 媒体相关服务实现
 * @date 2025-08-18 11:10
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final FnMediaInfoRepository fnMediaInfoRepository;

    private final AppConfig appConfig;

    private final MediaInfoCache mediaInfoCache;

    private final MediaInfoAssembler mediaInfoAssembler;

    @Override
    public void getM3u8File(String mediaGuid, ServletOutputStream outputStream) {
        MediaInfoDTO mediainfo = getMediaInfo(mediaGuid);
        if (Objects.nonNull(mediainfo) && StringUtils.isNotBlank(mediainfo.getM3u8Content())) {
            try {
                outputStream.write(mediainfo.getM3u8Content().getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                log.error("写入M3U8文件时发生错误", e);
            }
        }

    }

    private MediaInfoDTO getMediaInfo(String mediaGuid) {
        MediaInfoDTO cache = mediaInfoCache.getCache(mediaGuid);
        if (Objects.nonNull(cache)) {
            return cache;
        }
        FnMediaInfoDO mediaInfo = fnMediaInfoRepository.getByMediaGuid(mediaGuid);
        if (Objects.nonNull(mediaInfo)) {
            MediaInfoDTO mediaInfoDTO = mediaInfoAssembler.toMediaInfoDTO(mediaInfo);
            mediaInfoCache.saveCache(mediaGuid, mediaInfoDTO);
            return mediaInfoDTO;
        }
        return null;
    }

    @Override
    public void getTsFile(String mediaGuid, String fileName, ServletOutputStream outputStream) {
        MediaInfoDTO mediaInfo = getMediaInfo(mediaGuid);

        if (Objects.nonNull(mediaInfo)) {
            String mediaFullPath = mediaInfo.getMediaFullPath();
            FFmpegTranscodingUtil.sliceMediaToTs(appConfig.getFfmpegPath(), mediaFullPath, outputStream,
                    true, fileName, appConfig.getSegmentDuration(), mediaInfo.getAvgFrameRate());
        }
    }

    @Override
    public void saveOrUpdateMediaInfo(MediaInfoSaveRequest request) {
        FnMediaInfoDO mediaInfo = fnMediaInfoRepository.getByMediaGuid(request.getMediaGuid());
        String m3u8Content = M3u8Util.generateM3u8Content(BigDecimal.valueOf(request.getMediaDuration()),
                BigDecimal.valueOf(appConfig.getSegmentDuration()).divide(BigDecimal.valueOf(1000),2, RoundingMode.HALF_UP));
        if (Objects.nonNull(mediaInfo)) {
            mediaInfo
                    .setMediaName(request.getMediaName())
                    .setMediaFullPath(request.getMediaFullPath())
                    .setMediaDuration(request.getMediaDuration())
                    .setMediaFormat(request.getMediaFormat())
                    .setCategory(request.getCategory())
                    .setAvgFrameRate(request.getAvgFrameRate())
                    .setM3u8Content(m3u8Content);
            fnMediaInfoRepository.updateById(mediaInfo);
            return;
        }
        mediaInfo = FnMediaInfoDO.builder()
                .mediaGuid(request.getMediaGuid())
                .mediaName(request.getMediaName())
                .mediaFullPath(request.getMediaFullPath())
                .mediaType(request.getMediaType())
                .mediaFormat(request.getMediaFormat())
                .mediaDuration(request.getMediaDuration())
                .category(request.getCategory())
                .m3u8Content(m3u8Content)
                .avgFrameRate(request.getAvgFrameRate())
                .build();
        fnMediaInfoRepository.save(mediaInfo);
        // 更新缓存
        updateCache(mediaInfo);
    }

    private void updateCache(FnMediaInfoDO mediaInfo) {
        MediaInfoDTO cache = mediaInfoCache.getCache(mediaInfo.getMediaGuid());
        if (Objects.nonNull(cache)) {
            MediaInfoDTO mediaInfoDTO = mediaInfoAssembler.toMediaInfoDTO(mediaInfo);
            mediaInfoCache.saveCache(mediaInfo.getMediaGuid(), mediaInfoDTO);
        }
    }

    @Override
    public PlayResponse getPlayResponse(String mediaGuid) {
        return PlayResponse.builder().playLink("/v/media/" + mediaGuid + "/preset.m3u8").build();
    }
}
