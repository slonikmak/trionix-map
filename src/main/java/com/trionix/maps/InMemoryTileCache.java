package com.trionix.maps;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javafx.scene.image.Image;

/**
 * Simple in-memory LRU cache backed by {@link LinkedHashMap}.
 */
public final class InMemoryTileCache implements TileCache {

    private final int capacity;
    private final Map<TileKey, Image> cache;

    public InMemoryTileCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.cache = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<TileKey, Image> eldest) {
                return size() > InMemoryTileCache.this.capacity;
            }
        };
    }

    @Override
    public Image get(int zoom, long x, long y) {
        synchronized (cache) {
            return cache.get(new TileKey(zoom, x, y));
        }
    }

    @Override
    public void put(int zoom, long x, long y, Image image) {
        Objects.requireNonNull(image, "image");
        synchronized (cache) {
            cache.put(new TileKey(zoom, x, y), image);
        }
    }

    @Override
    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }

    private record TileKey(int zoom, long x, long y) {
    }
}
