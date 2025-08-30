package com.jankinwu.fntv.desktop.backend.task;

import com.jankinwu.fntv.desktop.backend.cache.ActivityTracker;
import com.jankinwu.fntv.desktop.backend.lock.MediaProcessingLock;
import com.jankinwu.fntv.desktop.backend.utils.MemoryCacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CacheCleanTask {

    @Scheduled(fixedRate = 2 * 60 * 1000)
    public void cleanAllCache() {
        // 检查系统是否处于活跃状态，避免频繁清理缓存
        if (!ActivityTracker.isActive()) {
            return;
        }

        try {
            // 获取缓存清理锁（阻塞等待）
            MediaProcessingLock.acquireCacheClearLock();
            log.info("开始执行缓存清理任务");
            try {
                MemoryCacheUtil.clearMemoryCache();
                log.info("缓存清理任务执行完成");
            } catch (Exception e) {
                log.error("缓存清理任务执行异常", e);
            }
        } catch (Exception e) {
            log.warn("获取缓存清理锁时被中断");
            Thread.currentThread().interrupt();
        } finally {
            // 释放缓存清理锁
            MediaProcessingLock.releaseCacheClearLock();
        }
    }
}
