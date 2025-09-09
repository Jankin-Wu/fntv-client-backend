package com.jankinwu.fntv.client.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * @author jankinwu
 * @description 定义一个注册器策略类, 方便后续加载资源文件
 * @date 2024/4/4 16:10
 */
@Component
public class ApplicationContextRegister implements ApplicationContextAware {

    private ApplicationContext applicationContext = null;

    /**
     * Spring容器启动时，会回调setApplicationContext方法，并传入ApplicationContext对象，之后就可对该对象进行操作。（例如获取spring容器中的所有bean）
     *
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    /**
     * 提供一个方法,用于加载sql脚本文件
     *
     * @param url sql文件位置
     * @return Resource
     */
    public Resource getResource(String url) {
        return this.applicationContext.getResource(url);
    }
}
