package com.jankinwu.fntv.client.service.impl;

import com.jankinwu.fntv.client.assembler.MediaInfoAssembler;
import com.jankinwu.fntv.client.cache.MediaInfoCache;
import com.jankinwu.fntv.client.dto.MediaInfoDTO;
import com.jankinwu.fntv.client.dto.req.MediaInfoSaveRequest;
import com.jankinwu.fntv.client.dto.req.PlayRequest;
import com.jankinwu.fntv.client.repository.FnMediaInfoRepository;
import com.jankinwu.fntv.client.repository.MediaTranscodingInfoRepository;
import com.jankinwu.fntv.client.repository.domain.MediaTranscodingInfoDO;
import com.jankinwu.fntv.client.utils.FFmpegTranscodingUtil;
import com.jankinwu.fntv.client.cache.ActivityTracker;
import com.jankinwu.fntv.client.cache.DeviceInfoHolder;
import com.jankinwu.fntv.client.config.AppConfig;
import com.jankinwu.fntv.client.config.TranscodingModeConfig;
import com.jankinwu.fntv.client.dto.CodecDTO;
import com.jankinwu.fntv.client.dto.resp.PlayResponse;
import com.jankinwu.fntv.client.lock.MediaProcessingLock;
import com.jankinwu.fntv.client.repository.domain.FnMediaInfoDO;
import com.jankinwu.fntv.client.service.MediaService;
import com.jankinwu.fntv.client.utils.M3u8Util;
import jakarta.servlet.ServletOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    private final DeviceInfoHolder deviceInfoHolder;

    private final MediaTranscodingInfoRepository mediaTranscodingInfoRepository;

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
        // 添加活动状态更新
        ActivityTracker.updateActivity();

        // 获取视频处理锁（阻塞等待），锁超时后会强制执行，避免阻塞
        try {
            MediaProcessingLock.acquireProcessingLock();
        } catch (InterruptedException e) {
            log.warn("获取视频处理锁时被中断");
            Thread.currentThread().interrupt();
            return;
        }

        try {
            MediaInfoDTO mediaInfo = getMediaInfo(mediaGuid);
            if (Objects.isNull(mediaInfo)) {
                return;
            }
            MediaTranscodingInfoDO mediaTranscodingInfoDO = mediaTranscodingInfoRepository.queryByMediaGuid(mediaGuid);
            // 设置转码模式
            if (Objects.nonNull(mediaTranscodingInfoDO) && StringUtils.isNotBlank(mediaTranscodingInfoDO.getTranscodingMode())) {
                TranscodingModeConfig.getInstance().setTranscodingMode(mediaTranscodingInfoDO.getTranscodingMode());
            } else {
                TranscodingModeConfig.getInstance().setTranscodingMode(appConfig.getTranscodingMode());
            }
            boolean enableHwTranscoding = TranscodingModeConfig.getInstance().isEnableHwAccel();
            log.info("当前转码模式为：{}", enableHwTranscoding ? "硬件加速" : "软件转码");
            String mediaFullPath = mediaInfo.getMediaFullPath();
            CodecDTO codec = null;
            if (StringUtils.isNotBlank(mediaInfo.getCodecName())) {
                codec = deviceInfoHolder.getCodec(mediaInfo.getCodecName());
            }
            try {
                FFmpegTranscodingUtil.sliceMediaToTs(appConfig.getFfmpegPath(), mediaFullPath, outputStream,
                        enableHwTranscoding, fileName, appConfig.getSegmentDuration(), mediaInfo.getAvgFrameRate(),
                        codec, mediaInfo.getColorPrimaries(), mediaInfo.getCodecName(), deviceInfoHolder.getHwAccelApi());
            } catch (IOException e) {
                log.error("获取ts文件时发生错误", e);
            } finally {
                try {
                    outputStream.flush();
                } catch (IOException e) {
                    log.error("刷新输出流时发生错误", e);
                }
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.error("关闭输出流时发生错误", e);
                }
            }
        } finally {
            // 释放视频处理锁
            MediaProcessingLock.releaseProcessingLock();
        }
    }

    @Override
    public void saveOrUpdateMediaInfo(MediaInfoSaveRequest request) {
        FnMediaInfoDO mediaInfo = fnMediaInfoRepository.getByMediaGuid(request.getMediaGuid());
        String m3u8Content = M3u8Util.generateM3u8Content(BigDecimal.valueOf(request.getMediaDuration()),
                BigDecimal.valueOf(appConfig.getSegmentDuration()).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP));
        if (Objects.nonNull(mediaInfo)) {
            mediaInfo
                    .setMediaName(request.getMediaName())
                    .setMediaFullPath(request.getMediaFullPath())
                    .setMediaDuration(request.getMediaDuration())
                    .setMediaFormat(request.getMediaFormat())
                    .setCategory(request.getCategory())
                    .setAvgFrameRate(request.getAvgFrameRate())
                    .setCodecName(request.getCodecName())
                    .setColorPrimaries(request.getColorPrimaries())
                    .setM3u8Content(m3u8Content);
            fnMediaInfoRepository.updateById(mediaInfo);
        } else {
            mediaInfo = mediaInfoAssembler.toFnMediaInfoDO(request);
            mediaInfo.setM3u8Content(m3u8Content);
            fnMediaInfoRepository.save(mediaInfo);
        }
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

    @Override
    public void saveOrUpdatePlayInfo(PlayRequest request) {
        MediaTranscodingInfoDO mediaTranscodingInfo = mediaTranscodingInfoRepository.queryByMediaGuid(request.getMediaGuid());
        if (Objects.isNull(mediaTranscodingInfo)) {
            mediaTranscodingInfo = MediaTranscodingInfoDO.builder()
                    .mediaGuid(request.getMediaGuid())
                    .transcodingMode(request.getTranscodingMode())
                    .build();
            mediaTranscodingInfoRepository.save(mediaTranscodingInfo);
            return;
        }
        mediaTranscodingInfo.setTranscodingMode(request.getTranscodingMode());
        mediaTranscodingInfoRepository.updateById(mediaTranscodingInfo);
    }
}
