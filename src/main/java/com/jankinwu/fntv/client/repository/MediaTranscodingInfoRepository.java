package com.jankinwu.fntv.client.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jankinwu.fntv.client.repository.domain.MediaTranscodingInfoDO;

/**
* @author Jankin-Wu
* @description 针对表【MEDIA_TRANSCODING_INFO(转码信息表)】的数据库操作Service
* @createDate 2025-08-23 16:42:28
*/
public interface MediaTranscodingInfoRepository extends IService<MediaTranscodingInfoDO> {

    MediaTranscodingInfoDO queryByMediaGuid(String mediaGuid);
}
