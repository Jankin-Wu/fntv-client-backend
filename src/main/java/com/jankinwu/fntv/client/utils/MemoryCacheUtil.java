package com.jankinwu.fntv.client.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MemoryCacheUtil {

    /**
     * 清理页缓存、目录项缓存和 inode 缓存
     */
    public static void clearMemoryCache() {
        try {
            // 使用ProcessBuilder执行sync命令
            ProcessBuilder syncPb = new ProcessBuilder("sync");
            syncPb.redirectErrorStream(true);
            Process syncProcess = syncPb.start();
            syncProcess.waitFor();

            // 使用ProcessBuilder执行drop_caches命令
            ProcessBuilder dropCachesPb = new ProcessBuilder("sh", "-c", "echo 3 > /proc/sys/vm/drop_caches");
            dropCachesPb.redirectErrorStream(true);
            Process dropCachesProcess = dropCachesPb.start();
            dropCachesProcess.waitFor();

            log.info("内存缓存清理完成");
        } catch (Exception e) {
            log.error("清理内存缓存时发生错误", e);
        }
    }
}
