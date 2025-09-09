package com.jankinwu.fntv.client.service;

public interface BuildIndexTaskService {
    void saveOrUpdateMediaInfo(String mediaGuid, String mediaName, String mediaFullPath, String mediaType,
                               String mediaFormat, Long duration, String category, Integer fps);
}
