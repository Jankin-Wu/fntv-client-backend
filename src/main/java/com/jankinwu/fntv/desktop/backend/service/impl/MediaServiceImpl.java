package com.jankinwu.fntv.desktop.backend.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.jankinwu.fntv.desktop.backend.config.AppConfig;
import com.jankinwu.fntv.desktop.backend.dto.req.PlayRequest;
import com.jankinwu.fntv.desktop.backend.dto.resp.PlayResponse;
import com.jankinwu.fntv.desktop.backend.enums.HlsFileEnum;
import com.jankinwu.fntv.desktop.backend.repository.FnMediaInfoRepository;
import com.jankinwu.fntv.desktop.backend.repository.domain.FnMediaInfoDO;
import com.jankinwu.fntv.desktop.backend.service.MediaService;
import com.jankinwu.fntv.desktop.backend.utils.FFmpegUtil;
import jakarta.servlet.ServletOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Map;
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

    @Override
    public void getM3u8File(String mediaGuid, ServletOutputStream outputStream) {
        FnMediaInfoDO mediaInfo = fnMediaInfoRepository.getByMediaGuid(mediaGuid);
        if (Objects.nonNull(mediaInfo)) {
            String m3u8Content = mediaInfo.getM3u8Content();
            try {
                // 创建临时文件
                File tempFile = File.createTempFile("m3u8_", ".m3u8");

                // 将m3u8内容写入临时文件
                try (FileWriter fileWriter = new FileWriter(tempFile);
                     BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                    bufferedWriter.write(m3u8Content);
                }

                // 将文件内容写入输出流
                try (FileInputStream fis = new FileInputStream(tempFile);
                     BufferedInputStream bis = new BufferedInputStream(fis)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                // 删除临时文件
                tempFile.delete();
            } catch (IOException e) {
                log.error("处理M3U8文件时发生错误", e);
//                throw new RuntimeException("处理M3U8文件时发生错误", e);
            }
        }
    }

    @Override
    public void getTsFile(String mediaGuid, String fileName, ServletOutputStream outputStream) {
        FnMediaInfoDO mediaInfo = fnMediaInfoRepository.getByMediaGuid(mediaGuid);

        if (Objects.nonNull(mediaInfo)) {
            String mediaFullPath = mediaInfo.getMediaFullPath();
            String tsStartTimeMapStr = mediaInfo.getTsStartTimeMap();
            if (StringUtils.isNotBlank(tsStartTimeMapStr)) {
                Map tsStartTimeMap = JSONObject.parseObject(tsStartTimeMapStr, Map.class);
                Long startTime = Long.valueOf(tsStartTimeMap.get(fileName).toString());
                int nextFileNumber = FFmpegUtil.parseFileNumberFromTsName(fileName) + 1;
                String nextFileName = String.format("%05d", nextFileNumber) + HlsFileEnum.TS.getSuffix();
                Long endTime = Long.valueOf(tsStartTimeMap.get(nextFileName).toString());
                FFmpegUtil.getTsFile(mediaFullPath, fileName, outputStream, appConfig.getSegmentDuration(),
                        appConfig.getFfmpegPath(), mediaInfo.getM3u8Content(), startTime, endTime);
            }
        }
    }

    @Override
    public void saveOrUpdateMediaInfo(PlayRequest request) {
        FnMediaInfoDO mediaInfo = fnMediaInfoRepository.getByMediaGuid(request.getMediaGuid());
        if (Objects.isNull(mediaInfo)) {
            mediaInfo = FnMediaInfoDO.builder()
                    .mediaGuid(request.getMediaGuid())
                    .mediaFullPath(request.getVideoPath())
                    .build();
            fnMediaInfoRepository.save(mediaInfo);
            return;
        }
        fnMediaInfoRepository.updateById(mediaInfo);
    }

    @Override
    public PlayResponse getPlayResponse(String mediaGuid) {
        return PlayResponse.builder().playLink("/v/media/" + mediaGuid + "/preset.m3u8").build();
    }
}
