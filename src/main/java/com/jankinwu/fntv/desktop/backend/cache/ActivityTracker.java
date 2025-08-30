package com.jankinwu.fntv.desktop.backend.cache;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class ActivityTracker {

    private static final AtomicReference<Instant> lastActivityTime = new AtomicReference<>(Instant.now());
    private static final long INACTIVITY_THRESHOLD = 30 * 60 * 1000; // 30分钟无活动则认为不活跃

    public static void updateActivity() {
        lastActivityTime.set(Instant.now());
    }

    public static boolean isActive() {
        return Instant.now().minusMillis(INACTIVITY_THRESHOLD).isBefore(lastActivityTime.get());
    }
}
