package com.trionix.maps.internal.tiles;

import com.trionix.maps.TileCache;
import com.trionix.maps.TileRetriever;
import com.trionix.maps.internal.concurrent.TileExecutors;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
    private final Map<TileCoordinate, LoadHandle> inFlight = new ConcurrentHashMap<>();
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
                deliverOnFxThread(coordinate, cached, consumer, generation);
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
        inFlight.compute(coordinate, (key, existing) -> {
            if (existing != null && existing.generation == generation) {
                return existing;
            }
                    CompletableFuture<Image> future = CompletableFuture
                        .supplyAsync(() -> retriever.loadTile(coordinate.zoom(), coordinate.x(), coordinate.y()),
                            TileExecutors.tileExecutor())
                        .thenCompose(tileFuture -> Objects.requireNonNull(tileFuture,
                            "TileRetriever returned null future"));
            future.whenComplete((image, error) ->
                    handleCompletion(coordinate, generation, image, error, consumer));
            return new LoadHandle(generation, future);
        });
    }

    private void handleCompletion(TileCoordinate coordinate, int generation,
            Image image, Throwable error, TileConsumer consumer) {
        inFlight.compute(coordinate, (key, handle) -> {
            if (handle == null || handle.generation != generation) {
                return handle;
            }
            return null;
        });

        if (generation != currentGeneration) {
            return; // stale result
        }

        if (error != null) {
            LOG.warn("Failed to load tile z={}, x={}, y={}",
                    coordinate.zoom(), coordinate.x(), coordinate.y(), error);
            return;
        }

        if (image != null) {
            cache.put(coordinate.zoom(), coordinate.x(), coordinate.y(), image);
            deliverOnFxThread(coordinate, image, consumer, generation);
        }
    }

    private void deliverOnFxThread(TileCoordinate coordinate, Image image,
            TileConsumer consumer, int generation) {
        Runnable delivery = () -> {
            if (generation == currentGeneration) {
                consumer.onTileLoaded(coordinate, image);
            }
        };
        if (Platform.isFxApplicationThread()) {
            delivery.run();
        } else {
            Platform.runLater(delivery);
        }
    }

    private record LoadHandle(int generation, CompletableFuture<Image> future) {
    }

    @FunctionalInterface
    public interface TileConsumer {
        void onTileLoaded(TileCoordinate coordinate, Image image);
    }
}
