package com.trionix.maps.internal.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Shared executor service for tile loading and decoding work.
 * <p>
 * Uses virtual threads with a concurrency limiter to prevent overwhelming
 * HTTP/2 connections with too many concurrent streams (servers typically
 * limit to ~100 streams per connection).
 */
public final class TileExecutors {

    /**
     * Maximum concurrent tile loading tasks. Using HTTP/1.1 with blocking
     * calls, so we can safely increase parallelism for faster loading.
     */
    private static final int MAX_CONCURRENT_TILES = 12;

    private static final Semaphore LIMITER = new Semaphore(MAX_CONCURRENT_TILES);

    private static final ExecutorService TILE_EXECUTOR =
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                    .name("tile-worker-", 0)
                    .factory());

    private TileExecutors() {
    }

    /**
     * Returns the shared tile executor. Tasks submitted here run on virtual
     * threads but are throttled by an internal semaphore to limit concurrent
     * network requests.
     */
    public static ExecutorService tileExecutor() {
        return TILE_EXECUTOR;
    }

    /**
     * Returns the semaphore used to limit concurrent tile operations.
     * Callers should acquire a permit before starting I/O-bound work and
     * release it when finished.
     */
    public static Semaphore concurrencyLimiter() {
        return LIMITER;
    }
}
