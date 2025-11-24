package com.trionix.maps.internal.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shared executor service for tile loading and decoding work.
 */
public final class TileExecutors {

    private static final ExecutorService TILE_EXECUTOR =
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                    .name("tile-worker-", 0)
                    .factory());

    private TileExecutors() {
    }

    public static ExecutorService tileExecutor() {
        return TILE_EXECUTOR;
    }
}
