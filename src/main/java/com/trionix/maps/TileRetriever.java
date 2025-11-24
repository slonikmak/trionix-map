package com.trionix.maps;

import java.util.concurrent.CompletableFuture;
import javafx.scene.image.Image;

/**
 * Strategy interface for asynchronously loading raster tiles. Implementations must be thread-safe
 * because {@link #loadTile(int, long, long)} can be invoked concurrently from background tile
 * loading threads.
 */
public interface TileRetriever {

    /**
     * Loads a single tile image for the specified zoom/x/y indices.
     *
     * @param zoom zoom level
     * @param x    tile x index
     * @param y    tile y index
     * @return a future that completes with the decoded {@link Image}; implementations may complete
     *         exceptionally to signal failures, in which case the {@code MapView} will render a
     *         placeholder tile and log the error
     */
    CompletableFuture<Image> loadTile(int zoom, long x, long y);
}
