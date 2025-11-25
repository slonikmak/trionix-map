package com.trionix.maps.internal.tiles;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.InMemoryTileCache;
import com.trionix.maps.TileRetriever;
import com.trionix.maps.testing.FxTestHarness;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TileManagerTest {

    private static Image sampleImage;

    @BeforeAll
    static void setupFxToolkit() {
        FxTestHarness.runOnFxThread(() -> {
        });
        sampleImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
    }

    @AfterAll
    static void tearDown() {
        sampleImage = null;
    }

    @Test
    void deliversCachedTileWithoutNetworkRequest() throws InterruptedException {
        InMemoryTileCache cache = new InMemoryTileCache(10);
        cache.put(1, 0, 0, sampleImage);
        RecordingRetriever retriever = new RecordingRetriever();
        TileManager manager = new TileManager(cache, retriever);

        CountDownLatch latch = new CountDownLatch(1);
        TileCoordinate coordinate = new TileCoordinate(1, 0, 0);

        manager.refreshTiles(List.of(coordinate), (tile, image) -> {
            assertThat(tile).isEqualTo(coordinate);
            assertThat(image).isSameAs(sampleImage);
            latch.countDown();
        });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(retriever.requestCount()).isZero();
    }

    @Test
    void cachesLoadedTileAndNotifiesConsumer() throws InterruptedException {
        InMemoryTileCache cache = new InMemoryTileCache(10);
        RecordingRetriever retriever = new RecordingRetriever();
        TileManager manager = new TileManager(cache, retriever);
        TileCoordinate coordinate = new TileCoordinate(2, 1, 1);
        CountDownLatch latch = new CountDownLatch(1);

        manager.refreshTiles(List.of(coordinate), (tile, image) -> {
            assertThat(tile).isEqualTo(coordinate);
            assertThat(image).isSameAs(sampleImage);
            latch.countDown();
        });

        LoadRequest request = retriever.takeRequest(Duration.ofSeconds(1));
        request.future().complete(sampleImage);

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(cache.get(2, 1, 1)).isSameAs(sampleImage);
    }

    @Test
    void reusesInFlightRequestForSameTile() throws InterruptedException {
        // When the same tile is requested while already loading, no duplicate
        // network request should be made - the existing in-flight request is reused.
        InMemoryTileCache cache = new InMemoryTileCache(10);
        RecordingRetriever retriever = new RecordingRetriever();
        TileManager manager = new TileManager(cache, retriever);
        TileCoordinate coordinate = new TileCoordinate(3, 4, 5);

        CountDownLatch firstLatch = new CountDownLatch(1);
        CountDownLatch secondLatch = new CountDownLatch(1);

        // First request starts loading
        manager.refreshTiles(List.of(coordinate), (tile, image) -> firstLatch.countDown());
        LoadRequest firstRequest = retriever.takeRequest(Duration.ofSeconds(1));

        // Second request for same tile while first is in-flight
        manager.refreshTiles(List.of(coordinate), (tile, image) -> secondLatch.countDown());
        
        // Should NOT create another network request
        Thread.sleep(100); // Give time for any spurious request to arrive
        assertThat(retriever.requestCount()).isZero(); // No new requests in queue

        // Complete the original request
        Image image = FxTestHarness.callOnFxThread(() -> new WritableImage(32, 32));
        firstRequest.future().complete(image);

        // First consumer should receive the tile
        assertThat(firstLatch.await(1, TimeUnit.SECONDS)).isTrue();
        // Tile should be cached
        assertThat(cache.get(3, 4, 5)).isSameAs(image);
    }

    private static final class RecordingRetriever implements TileRetriever {
        private final BlockingQueue<LoadRequest> requests = new LinkedBlockingDeque<>();

        @Override
        public java.util.concurrent.CompletableFuture<Image> loadTile(int zoom, long x, long y) {
            var future = new java.util.concurrent.CompletableFuture<Image>();
            requests.add(new LoadRequest(new TileCoordinate(zoom, x, y), future));
            return future;
        }

        int requestCount() {
            return requests.size();
        }

        LoadRequest takeRequest(Duration timeout) throws InterruptedException {
            LoadRequest request = requests.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return Objects.requireNonNull(request, "Timed out waiting for tile request");
        }
    }

    private record LoadRequest(TileCoordinate coordinate,
                               java.util.concurrent.CompletableFuture<Image> future) {
    }
}
