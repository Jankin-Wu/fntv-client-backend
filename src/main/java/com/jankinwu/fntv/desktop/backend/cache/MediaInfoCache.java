package com.jankinwu.fntv.desktop.backend.cache;

import com.jankinwu.fntv.desktop.backend.dto.MediaInfoDTO;

/**
 * @author Jankin-Wu
 * @description 媒体信息缓存
 * @date 2025-08-21 10:51
 **/
public interface MediaInfoCache {

    void saveCache(String mediaGuid, MediaInfoDTO mediaInfoDO);

    MediaInfoDTO getCache(String mediaGuid);
}
