package com.jankinwu.fntv.client.assembler;


import com.jankinwu.fntv.client.dto.MediaInfoDTO;
import com.jankinwu.fntv.client.dto.req.MediaInfoSaveRequest;
import com.jankinwu.fntv.client.repository.domain.FnMediaInfoDO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * @author Jankin-Wu
 * @description 媒体信息组装器
 * @date 2025-08-21 11:50
 **/
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface MediaInfoAssembler {

    MediaInfoDTO toMediaInfoDTO(FnMediaInfoDO mediaInfoDO);

    FnMediaInfoDO toFnMediaInfoDO(MediaInfoSaveRequest request);
}
