package com.jankinwu.fntv.desktop.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;

/**
 * @author jankinwu
 * @description H2数据库数据源配置
 * @date 2024/4/4 15:46
 */
@Slf4j
@Service
@AutoConfigureAfter(DataSource.class)
public class H2DataSourceConfig {

    /**
     * 初始化sql
     */
    private static final String SCHEMA = "classpath:db/fntv-ddl.sql";

    private final DataSource dataSource;

    private final ApplicationContextRegister applicationContextRegister;

    @Autowired
    public H2DataSourceConfig(DataSource dataSource, ApplicationContextRegister applicationContextRegister) {
        this.dataSource = dataSource;
        this.applicationContextRegister = applicationContextRegister;
    }


    @PostConstruct
    public void init() throws Exception {
        //初始化本地数据库
        //获取系统用户目录
        String userHome = System.getProperty("user.dir");
        // 创建一个标识文件,只有在第一次初始化数据库时会创建,如果系统用户目录下有这个文件,就不会重新执行sql脚本
        File f = new File(userHome + File.separator + "fn_tv_db.lock");
        if (!f.exists()) {
            log.info("--------------初始化h2数据库----------------------");
            f.createNewFile();
            // 加载资源文件
            Resource resource = applicationContextRegister.getResource(SCHEMA);
            // 手动执行SQL语句
            ScriptUtils.executeSqlScript(dataSource.getConnection(), resource);
            log.info("--------------h2数据库初始化完成----------------------");
        }
    }
}
