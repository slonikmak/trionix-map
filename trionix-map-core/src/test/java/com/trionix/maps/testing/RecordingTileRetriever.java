package com.trionix.maps.testing;

import com.trionix.maps.TileRetriever;
import com.trionix.maps.internal.tiles.TileCoordinate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import javafx.scene.image.Image;

/**
 * Test double that records tile load requests and exposes their completion futures.
 */
public final class RecordingTileRetriever implements TileRetriever {

    private final BlockingQueue<LoadRequest> requests = new LinkedBlockingDeque<>();

    @Override
    public CompletableFuture<Image> loadTile(int zoom, long x, long y) {
        CompletableFuture<Image> future = new CompletableFuture<>();
        requests.add(new LoadRequest(new TileCoordinate(zoom, x, y), future));
        return future;
    }

    public int requestCount() {
        return requests.size();
    }

    public LoadRequest takeRequest(Duration timeout) throws InterruptedException {
        long millis = Objects.requireNonNull(timeout, "timeout").toMillis();
        LoadRequest request = requests.poll(millis, TimeUnit.MILLISECONDS);
        if (request == null) {
            throw new IllegalStateException("Timed out waiting for tile request");
        }
        return request;
    }

    public List<LoadRequest> awaitRequests(int count, Duration timeoutPerRequest) throws InterruptedException {
        if (count < 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        List<LoadRequest> captured = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            captured.add(takeRequest(timeoutPerRequest));
        }
        return captured;
    }

    public record LoadRequest(TileCoordinate coordinate, CompletableFuture<Image> future) {
    }
}
