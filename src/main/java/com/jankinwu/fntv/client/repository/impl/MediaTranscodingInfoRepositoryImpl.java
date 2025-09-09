package com.jankinwu.fntv.client.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jankinwu.fntv.client.repository.MediaTranscodingInfoRepository;
import com.jankinwu.fntv.client.repository.domain.MediaTranscodingInfoDO;
import com.jankinwu.fntv.client.mapper.MediaTranscodingInfoMapper;
import org.springframework.stereotype.Service;

/**
* @author Jankin-Wu
* @description 针对表【MEDIA_TRANSCODING_INFO(转码信息表)】的数据库操作Service实现
* @createDate 2025-08-23 16:42:28
*/
@Service
public class MediaTranscodingInfoRepositoryImpl extends ServiceImpl<MediaTranscodingInfoMapper, MediaTranscodingInfoDO>
    implements MediaTranscodingInfoRepository {

    @Override
    public MediaTranscodingInfoDO queryByMediaGuid(String mediaGuid) {
        return lambdaQuery()
                .eq(MediaTranscodingInfoDO::getMediaGuid, mediaGuid)
                .one();
    }
}




