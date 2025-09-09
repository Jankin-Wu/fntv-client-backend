package com.jankinwu.fntv.client.repository;

import com.jankinwu.fntv.client.repository.domain.FnMediaInfoDO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Jankin-Wu
* @description 针对表【FN_MEDIA_INFO(飞牛媒体信息表)】的数据库操作Service
* @createDate 2025-08-19 17:12:48
*/
public interface FnMediaInfoRepository extends IService<FnMediaInfoDO> {

    FnMediaInfoDO getByMediaGuid(String mediaCode);
}
