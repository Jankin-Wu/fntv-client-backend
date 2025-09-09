package com.jankinwu.fntv.client.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 编码器
 */
@Data
@Accessors(chain = true)
public class CodecDTO {

    private String hwDecoderName;

    private String swDecoderName;

    private String hwEncoderName;

    private String swEncoderName;
}
