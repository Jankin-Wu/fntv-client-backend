package com.jankinwu.fntv.desktop.backend.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jankinwu.fntv.desktop.backend.mapper.FnMediaInfoMapper;
import com.jankinwu.fntv.desktop.backend.repository.FnMediaInfoRepository;
import com.jankinwu.fntv.desktop.backend.repository.domain.FnMediaInfoDO;
import org.springframework.stereotype.Service;

/**
* @author Jankin-Wu
* @description 针对表【FN_MEDIA_INFO(飞牛媒体信息表)】的数据库操作Service实现
* @createDate 2025-08-19 17:12:48
*/
@Service
public class FnMediaInfoRepositoryImpl extends ServiceImpl<FnMediaInfoMapper, FnMediaInfoDO>
    implements FnMediaInfoRepository {

    @Override
    public FnMediaInfoDO getByMediaCode(String mediaCode) {
        return lambdaQuery()
                .eq(FnMediaInfoDO::getMediaGuid, mediaCode)
                .one();
    }
}




