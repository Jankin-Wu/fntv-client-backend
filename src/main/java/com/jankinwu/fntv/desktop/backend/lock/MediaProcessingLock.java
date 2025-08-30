package com.jankinwu.fntv.desktop.backend.lock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class MediaProcessingLock {


    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final long LOCK_TIMEOUT_SECONDS = 20; // 锁等待超时时间

    /**
     * 获取视频处理锁（读锁）- 带超时
     */
    public static boolean acquireProcessingLock() throws InterruptedException {
        log.info("尝试获取视频处理锁，超时时间: {}秒", LOCK_TIMEOUT_SECONDS);
        boolean acquired = lock.readLock().tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (acquired) {
            log.info("成功获取视频处理锁");
        } else {
            log.warn("获取视频处理锁超时");
        }
        return acquired;
    }

    /**
     * 释放视频处理锁
     */
    public static void releaseProcessingLock() {
        try {
            lock.readLock().unlock();
            log.info("成功释放视频处理锁");
        } catch (Exception e) {
            log.error("释放视频处理锁时发生错误", e);
        }
    }

    /**
     * 获取缓存清理锁（写锁）- 带超时
     */
    public static boolean acquireCacheClearLock() throws InterruptedException {
        log.info("尝试获取缓存清理锁，超时时间: {}秒", LOCK_TIMEOUT_SECONDS);
        boolean acquired = lock.writeLock().tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (acquired) {
            log.info("成功获取缓存清理锁");
        } else {
            log.warn("获取缓存清理锁超时");
        }
        return acquired;
    }

    /**
     * 释放缓存清理锁
     */
    public static void releaseCacheClearLock() {
        try {
            lock.writeLock().unlock();
            log.info("成功释放缓存清理锁");
        } catch (Exception e) {
            log.error("释放缓存清理锁时发生错误", e);
        }
    }
}
