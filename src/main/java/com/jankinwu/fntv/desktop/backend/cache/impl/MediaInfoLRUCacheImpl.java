package com.jankinwu.fntv.desktop.backend.cache.impl;

import com.jankinwu.fntv.desktop.backend.cache.MediaInfoCache;
import com.jankinwu.fntv.desktop.backend.dto.MediaInfoDTO;
import lombok.Synchronized;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * @author Jankin-Wu
 * @description 媒体信息缓存实现
 * @date 2025-08-21 10:52
 **/
@Component
public class MediaInfoLRUCacheImpl implements MediaInfoCache {

    private static final Integer MAX_SIZE = 50;

    private static final LinkedHashMap<String, MediaInfoDTO> cacheMap = new LinkedHashMap<>();

    @Synchronized
    @Override
    public void saveCache(String mediaGuid, MediaInfoDTO mediaInfoDO) {
        MediaInfoDTO fnMediaInfo = cacheMap.get(mediaGuid);
        if (Objects.nonNull(fnMediaInfo)) {
            // 如果已存在，更新缓存
            cacheMap.put(mediaGuid, mediaInfoDO);
        } else {
            // 如果缓存已满，移除最老的条目
            if (!cacheMap.isEmpty() && cacheMap.size() >= MAX_SIZE) {
                String firstKey = cacheMap.keySet().iterator().next();
                cacheMap.remove(firstKey);
            }
            cacheMap.put(mediaGuid, mediaInfoDO);
        }
    }

    @Synchronized
    @Override
    public MediaInfoDTO getCache(String mediaGuid) {
        return cacheMap.get(mediaGuid);
    }
}
