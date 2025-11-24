package com.trionix.maps;

import javafx.scene.image.Image;

/**
 * Thread-safe cache for storing decoded tile images. Implementations are invoked from both
 * JavaFX and background tile-loading threads and therefore must synchronize appropriately.
 */
public interface TileCache {

    /**
     * Retrieves a tile if present.
     *
     * @return cached {@link Image} instance or {@code null}
     */
    Image get(int zoom, long x, long y);

    /**
     * Stores (or replaces) a tile image.
     */
    void put(int zoom, long x, long y, Image image);

    /**
     * Clears the cache contents.
     */
    void clear();
}
