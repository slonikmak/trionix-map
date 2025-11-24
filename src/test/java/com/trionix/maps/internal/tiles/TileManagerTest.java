package com.trionix.maps.internal.tiles;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.InMemoryTileCache;
import com.trionix.maps.TileRetriever;
import com.trionix.maps.internal.tiles.TileManager.TileConsumer;
import com.trionix.maps.testing.FxTestHarness;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    void ignoresTilesFromPreviousGeneration() throws InterruptedException {
        InMemoryTileCache cache = new InMemoryTileCache(10);
        RecordingRetriever retriever = new RecordingRetriever();
        TileManager manager = new TileManager(cache, retriever);
        TileCoordinate coordinate = new TileCoordinate(3, 4, 5);
        AtomicBoolean staleDelivered = new AtomicBoolean(false);

        manager.refreshTiles(List.of(coordinate), (tile, image) -> staleDelivered.set(true));
        LoadRequest firstRequest = retriever.takeRequest(Duration.ofSeconds(1));

        CountDownLatch freshLatch = new CountDownLatch(1);
        TileConsumer consumer = (tile, image) -> {
            if (tile.equals(coordinate)) {
                freshLatch.countDown();
            }
        };
        manager.refreshTiles(List.of(coordinate), consumer);
        LoadRequest secondRequest = retriever.takeRequest(Duration.ofSeconds(1));

        Image staleImage = FxTestHarness.callOnFxThread(() -> new WritableImage(16, 16));
        firstRequest.future().complete(staleImage);
        FxTestHarness.runOnFxThread(() -> {
        });
        assertThat(staleDelivered).isFalse();

        Image freshImage = FxTestHarness.callOnFxThread(() -> new WritableImage(32, 32));
        secondRequest.future().complete(freshImage);

        assertThat(freshLatch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(cache.get(3, 4, 5)).isSameAs(freshImage);
    }

    @Test
    void invokesTileRetrieverOnVirtualThread() throws Exception {
        InMemoryTileCache cache = new InMemoryTileCache(10);
        ThreadCapturingRetriever retriever = new ThreadCapturingRetriever();
        TileManager manager = new TileManager(cache, retriever);
        TileCoordinate coordinate = new TileCoordinate(4, 2, 1);

        FxTestHarness.runOnFxThread(() ->
                manager.refreshTiles(List.of(coordinate), (tile, image) -> {
                }));

        ThreadCall call = retriever.takeCall(Duration.ofSeconds(1));
        assertThat(call.thread().isVirtual()).isTrue();
        assertThat(call.thread().getName()).startsWith("tile-worker-");
        call.future().complete(sampleImage);
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

    private static final class ThreadCapturingRetriever implements TileRetriever {
        private final BlockingQueue<ThreadCall> calls = new LinkedBlockingDeque<>();

        @Override
        public CompletableFuture<Image> loadTile(int zoom, long x, long y) {
            CompletableFuture<Image> future = new CompletableFuture<>();
            calls.add(new ThreadCall(Thread.currentThread(), new TileCoordinate(zoom, x, y), future));
            return future;
        }

        ThreadCall takeCall(Duration timeout) throws InterruptedException {
            ThreadCall call = calls.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return Objects.requireNonNull(call, "Timed out waiting for retriever invocation");
        }
    }

    private record ThreadCall(Thread thread, TileCoordinate coordinate,
                              CompletableFuture<Image> future) {
    }
}
