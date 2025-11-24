package com.trionix.maps.internal.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared executor service for tile loading and decoding work.
 */
public final class TileExecutors {

    private static final ExecutorService TILE_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            new TileThreadFactory());

    private TileExecutors() {
    }

    public static ExecutorService tileExecutor() {
        return TILE_EXECUTOR;
    }

    private static final class TileThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "tile-worker-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
