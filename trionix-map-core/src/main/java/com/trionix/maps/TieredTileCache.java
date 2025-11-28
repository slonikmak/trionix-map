package com.trionix.maps;

import java.util.List;
import java.util.Objects;
import javafx.scene.image.Image;

/**
 * Composite tile cache that chains multiple {@link TileCache} instances in a tiered architecture.
 *
 * <p>Tiers are checked in order during {@link #get(int, long, long)}. When a tile is found in
 * a lower tier (e.g., L2 disk cache), it is automatically promoted to higher tiers (e.g., L1
 * memory cache) for faster subsequent access.
 *
 * <p>The {@link #put(int, long, long, Image)} method writes to all tiers to maintain consistency.
 *
 * <p>Example usage:
 * <pre>{@code
 * TileCache cache = new TieredTileCache(List.of(
 *     new InMemoryTileCache(500),           // L1: fast, 500 tiles
 *     new FileTileCache(cacheDir, 10_000)   // L2: persistent, 10k tiles
 * ));
 * }</pre>
 */
public final class TieredTileCache implements TileCache {

    private final List<TileCache> tiers;

    /**
     * Creates a tiered cache with the specified tiers.
     *
     * @param tiers ordered list of caches (L1 first, L2 second, etc.)
     * @throws IllegalArgumentException if tiers is null, empty, or contains null elements
     */
    public TieredTileCache(List<TileCache> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            throw new IllegalArgumentException("tiers must not be null or empty");
        }
        for (TileCache tier : tiers) {
            if (tier == null) {
                throw new IllegalArgumentException("tiers must not contain null elements");
            }
        }
        this.tiers = List.copyOf(tiers);
    }

    @Override
    public Image get(int zoom, long x, long y) {
        for (int i = 0; i < tiers.size(); i++) {
            Image image = tiers.get(i).get(zoom, x, y);
            if (image != null) {
                // Promote to higher tiers
                for (int j = 0; j < i; j++) {
                    tiers.get(j).put(zoom, x, y, image);
                }
                return image;
            }
        }
        return null;
    }

    @Override
    public void put(int zoom, long x, long y, Image image) {
        Objects.requireNonNull(image, "image");
        for (TileCache tier : tiers) {
            tier.put(zoom, x, y, image);
        }
    }

    @Override
    public void clear() {
        for (TileCache tier : tiers) {
            tier.clear();
        }
    }
}
