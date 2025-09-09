package com.jankinwu.fntv.client.service;

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
    void saveOrUpdateMediaInfo() {
        String mediaGuid = UUID.randomUUID().toString().replace("-", "");
        buildIndexTaskService.saveOrUpdateMediaInfo("d85a782b26e943c5bb9135f847c7bc66", "测试视频",
                "D:\\video\\BigBuckBunny.mp4", "TV", "MP4", 596L, "动画", 24);
    }
}