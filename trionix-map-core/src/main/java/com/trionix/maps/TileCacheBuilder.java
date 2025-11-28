package com.trionix.maps;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for constructing tile caches.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Memory-only cache
 * TileCache cache = TileCacheBuilder.create()
 *     .memory(500)
 *     .build();
 *
 * // Disk-only cache
 * TileCache cache = TileCacheBuilder.create()
 *     .disk(Path.of("~/.cache/myapp/tiles"), 10_000)
 *     .build();
 *
 * // Tiered cache (recommended)
 * TileCache cache = TileCacheBuilder.create()
 *     .memory(500)
 *     .disk(Path.of("~/.cache/myapp/tiles"), 10_000)
 *     .build();
 * }</pre>
 */
public final class TileCacheBuilder {

    private final List<TileCache> caches = new ArrayList<>();

    private TileCacheBuilder() {
    }

    /**
     * Creates a new tile cache builder.
     *
     * @return a new builder instance
     */
    public static TileCacheBuilder create() {
        return new TileCacheBuilder();
    }

    /**
     * Adds an in-memory cache tier.
     *
     * @param capacity the maximum number of tiles to store in memory
     * @return this builder for method chaining
     */
    public TileCacheBuilder memory(int capacity) {
        caches.add(new InMemoryTileCache(capacity));
        return this;
    }

    /**
     * Adds a disk cache tier.
     *
     * @param cacheDir the directory where tiles are stored
     * @param maxFiles the maximum number of tile files to store
     * @return this builder for method chaining
     */
    public TileCacheBuilder disk(Path cacheDir, int maxFiles) {
        caches.add(new FileTileCache(cacheDir, maxFiles));
        return this;
    }

    /**
     * Builds the configured tile cache.
     *
     * <p>If a single cache tier is configured, that cache is returned directly.
     * If multiple tiers are configured, a {@link TieredTileCache} wrapping all tiers is returned.
     *
     * @return the configured tile cache
     * @throws IllegalStateException if no cache tiers have been configured
     */
    public TileCache build() {
        if (caches.isEmpty()) {
            throw new IllegalStateException("No cache tiers configured. Use memory() or disk() to add cache tiers.");
        }
        if (caches.size() == 1) {
            return caches.get(0);
        }
        return new TieredTileCache(caches);
    }
}
