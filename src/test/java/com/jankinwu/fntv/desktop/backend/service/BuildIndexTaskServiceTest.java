package com.jankinwu.fntv.desktop.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class BuildIndexTaskServiceTest {

    @Autowired
    private BuildIndexTaskService buildIndexTaskService;

    @Test
    void saveMediaInfo() {
        String mediaGuid = UUID.randomUUID().toString().replace("-", "");
        buildIndexTaskService.saveMediaInfo(mediaGuid, "测试视频", "E:\\Vedio\\BigBuckBunny.mp4", "TV", "MP4", 1000L, "动画");
    }
}