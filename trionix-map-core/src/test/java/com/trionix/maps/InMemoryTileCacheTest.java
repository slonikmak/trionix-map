package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
class InMemoryTileCacheTest {

    private static Image sampleImage;

    @Start
    private void start(Stage stage) {
        // Initialize JavaFX toolkit
    }

    @BeforeAll
    static void setupImage() {
        // TestFX handles toolkit initialization via @Start
    }

    @AfterAll
    static void cleanup() {
        sampleImage = null;
    }

    private Image getSampleImage() {
        if (sampleImage == null) {
            sampleImage = new WritableImage(1, 1);
        }
        return sampleImage;
    }

    @Test
    void evictsLeastRecentlyUsedTile() {
        WaitForAsyncUtils.waitForFxEvents();
        Image image = getSampleImage();
        
        InMemoryTileCache cache = new InMemoryTileCache(2);
        cache.put(1, 1, 1, image);
        cache.put(1, 2, 2, image);
        cache.get(1, 1, 1); // mark as most recently used
        cache.put(1, 3, 3, image);

        assertThat(cache.get(1, 1, 1)).isNotNull();
        assertThat(cache.get(1, 2, 2)).isNull();
        assertThat(cache.get(1, 3, 3)).isNotNull();
    }

    @Test
    void supportsConcurrentAccess() throws InterruptedException {
        WaitForAsyncUtils.waitForFxEvents();
        Image image = getSampleImage();
        
        TileCache cache = new InMemoryTileCache(50);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> tasks = java.util.stream.IntStream.range(0, 8)
                    .mapToObj(worker -> (Callable<Void>) () -> {
                        for (int i = 0; i < 1000; i++) {
                            cache.put(worker, worker, worker, image);
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
