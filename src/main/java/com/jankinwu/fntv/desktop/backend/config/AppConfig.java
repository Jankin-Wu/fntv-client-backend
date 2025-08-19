package com.jankinwu.fntv.desktop.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Jankin-Wu刚
 * @description 项目配置
 * @date 2025-08-19 18:07
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    /**
     * 切片时长（单位：秒）
     */
    private Integer segmentDuration;
}
