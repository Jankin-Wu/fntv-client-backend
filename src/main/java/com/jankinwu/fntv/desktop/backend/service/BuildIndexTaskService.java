package com.jankinwu.fntv.desktop.backend.service;

public interface BuildIndexTaskService {
    void saveOrUpdateMediaInfo(String mediaGuid, String mediaName, String mediaFullPath, String mediaType, String mediaFormat, Long duration, String category);
}
