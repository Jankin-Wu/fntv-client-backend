package com.jankinwu.fntv.client.cache;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class ActivityTracker {

    private static final AtomicReference<Instant> lastActivityTime = new AtomicReference<>(null);
    private static final long INACTIVITY_THRESHOLD = 10 * 60 * 1000;

    public static void updateActivity() {
        lastActivityTime.set(Instant.now());
    }

    public static boolean isActive() {
        Instant lastTime = lastActivityTime.get();
        // 如果是初始值(null)，表示系统刚启动且没有实际活动，返回false
        if (lastTime == null) {
            return false;
        }
        // 否则按照正常逻辑判断是否活跃
        return Instant.now().minusMillis(INACTIVITY_THRESHOLD).isBefore(lastTime);
    }
}
