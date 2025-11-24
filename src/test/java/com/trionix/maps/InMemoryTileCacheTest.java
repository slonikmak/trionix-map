package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.testing.FxTestHarness;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InMemoryTileCacheTest {

    private static Image sampleImage;

    @BeforeAll
    static void setupImage() {
        sampleImage = FxTestHarness.callOnFxThread(() -> new WritableImage(1, 1));
    }

    @AfterAll
    static void cleanup() {
        sampleImage = null;
    }

    @Test
    void evictsLeastRecentlyUsedTile() {
        InMemoryTileCache cache = new InMemoryTileCache(2);
        cache.put(1, 1, 1, sampleImage);
        cache.put(1, 2, 2, sampleImage);
        cache.get(1, 1, 1); // mark as most recently used
        cache.put(1, 3, 3, sampleImage);

        assertThat(cache.get(1, 1, 1)).isNotNull();
        assertThat(cache.get(1, 2, 2)).isNull();
        assertThat(cache.get(1, 3, 3)).isNotNull();
    }

    @Test
    void supportsConcurrentAccess() throws InterruptedException {
        TileCache cache = new InMemoryTileCache(50);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> tasks = java.util.stream.IntStream.range(0, 8)
                    .mapToObj(worker -> (Callable<Void>) () -> {
                        for (int i = 0; i < 1000; i++) {
                            cache.put(worker, worker, worker, sampleImage);
                            cache.get(worker, worker, worker);
                        }
                        return null;
                    })
                    .toList();
            executor.invokeAll(tasks);
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        java.util.stream.IntStream.range(0, 8).forEach(worker ->
                assertThat(cache.get(worker, worker, worker)).isNotNull());
    }
}
