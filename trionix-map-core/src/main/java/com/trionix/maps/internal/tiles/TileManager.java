package com.trionix.maps.internal.tiles;

import com.trionix.maps.TileCache;
import com.trionix.maps.TileRetriever;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinates cache lookups, asynchronous tile retrieval, and generation-based
 * invalidation to ensure only current tiles are rendered.
 */
public final class TileManager {

    private static final Logger LOG = LoggerFactory.getLogger(TileManager.class);

    private final TileCache cache;
    private final TileRetriever retriever;
    private final Set<TileCoordinate> pendingRequests = ConcurrentHashMap.newKeySet();
    private final AtomicInteger generationCounter = new AtomicInteger();
    private volatile int currentGeneration;

    public TileManager(TileCache cache, TileRetriever retriever) {
        this.cache = Objects.requireNonNull(cache, "cache");
        this.retriever = Objects.requireNonNull(retriever, "retriever");
    }

    public int currentGeneration() {
        return currentGeneration;
    }

    /**
     * Refreshes the visible tile set. Cached tiles are delivered immediately
     * while missing tiles are loaded asynchronously. Returns the generation id
     * representing this refresh.
     */
    public int refreshTiles(List<TileCoordinate> desiredTiles, TileConsumer consumer) {
        Objects.requireNonNull(desiredTiles, "desiredTiles");
        Objects.requireNonNull(consumer, "consumer");
        int generation = generationCounter.incrementAndGet();
        currentGeneration = generation;

        // Preserve iteration order but drop duplicates to avoid redundant loads.
        Set<TileCoordinate> uniqueTiles = new LinkedHashSet<>(desiredTiles);
        for (TileCoordinate coordinate : uniqueTiles) {
            Image cached = cache.get(coordinate.zoom(), coordinate.x(), coordinate.y());
            if (cached != null) {
                deliverOnFxThread(coordinate, cached, consumer);
                continue;
            }
            scheduleLoad(coordinate, generation, consumer);
        }
        return generation;
    }

    /** Returns the cached tile image if available, otherwise {@code null}. */
    public Image cachedTile(TileCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "coordinate");
        return cache.get(coordinate.zoom(), coordinate.x(), coordinate.y());
    }

    public void clearCache() {
        cache.clear();
    }

    private void scheduleLoad(TileCoordinate coordinate, int generation, TileConsumer consumer) {
        // Check if already loading this tile
        if (!pendingRequests.add(coordinate)) {
            LOG.debug("Skipping duplicate load for z={}, x={}, y={}", 
                    coordinate.zoom(), coordinate.x(), coordinate.y());
            return; // Skip duplicate request
        }

        LOG.debug("Starting load for z={}, x={}, y={}", 
                coordinate.zoom(), coordinate.x(), coordinate.y());

        // Use the retriever's async method directly
        retriever.loadTile(coordinate.zoom(), coordinate.x(), coordinate.y())
                .whenComplete((image, error) -> {
                    // Always remove from pending requests
                    pendingRequests.remove(coordinate);

                    if (error != null) {
                        LOG.warn("Failed to load tile z={}, x={}, y={}",
                                coordinate.zoom(), coordinate.x(), coordinate.y(), error);
                        return;
                    }

                    if (image != null && !image.isError()) {
                        // Cache the tile
                        cache.put(coordinate.zoom(), coordinate.x(), coordinate.y(), image);
                        // Deliver to UI - even if generation changed, the MapView will
                        // use cached tiles during redraw and ignore tiles not in the
                        // current visible set
                        deliverOnFxThread(coordinate, image, consumer);
                    } else {
                        LOG.warn("Failed to decode tile z={}, x={}, y={}",
                                coordinate.zoom(), coordinate.x(), coordinate.y());
                    }
                });
    }

    private void deliverOnFxThread(TileCoordinate coordinate, Image image,
            TileConsumer consumer) {
        Runnable delivery = () -> consumer.onTileLoaded(coordinate, image);
        if (Platform.isFxApplicationThread()) {
            delivery.run();
        } else {
            Platform.runLater(delivery);
        }
    }

    @FunctionalInterface
    public interface TileConsumer {
        void onTileLoaded(TileCoordinate coordinate, Image image);
    }
}
