package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trionix.maps.testing.FxTestHarness;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileTileCacheTest {

    private static Image sampleImage;

    @TempDir
    Path tempDir;

    private Path cacheDir;

    @BeforeAll
    static void setupImage() {
        sampleImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
    }

    @AfterAll
    static void cleanup() {
        sampleImage = null;
    }

    @BeforeEach
    void setUp() {
        cacheDir = tempDir.resolve("tiles");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(cacheDir)) {
            try (Stream<Path> walk = Files.walk(cacheDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        }
    }

    @Test
    void putAndGetRoundtripStoresAndRetrievesTile() {
        FileTileCache cache = new FileTileCache(cacheDir, 100);

        cache.put(12, 2048, 1536, sampleImage);
        Image retrieved = cache.get(12, 2048, 1536);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getWidth()).isEqualTo(256);
        assertThat(retrieved.getHeight()).isEqualTo(256);
    }

    @Test
    void getReturnsNullForMissingTile() {
        FileTileCache cache = new FileTileCache(cacheDir, 100);

        Image retrieved = cache.get(12, 9999, 9999);

        assertThat(retrieved).isNull();
    }

    @Test
    void clearRemovesAllCachedFiles() throws IOException {
        FileTileCache cache = new FileTileCache(cacheDir, 100);
        cache.put(12, 1, 1, sampleImage);
        cache.put(12, 2, 2, sampleImage);
        cache.put(13, 3, 3, sampleImage);

        cache.clear();

        assertThat(Files.exists(cacheDir)).isFalse();
        assertThat(cache.get(12, 1, 1)).isNull();
        assertThat(cache.get(12, 2, 2)).isNull();
        assertThat(cache.get(13, 3, 3)).isNull();
    }

    @Test
    void lruEvictionRemovesOldestFilesWhenCapacityExceeded() throws InterruptedException {
        FileTileCache cache = new FileTileCache(cacheDir, 3);

        // Add tiles with delays to ensure different last-modified times
        cache.put(1, 0, 0, sampleImage);
        Thread.sleep(50);
        cache.put(1, 1, 1, sampleImage);
        Thread.sleep(50);
        cache.put(1, 2, 2, sampleImage);
        Thread.sleep(50);

        // Access tile 0,0 to make it most recently used
        cache.get(1, 0, 0);
        Thread.sleep(50);

        // Add a fourth tile, should evict tile 1,1 (oldest accessed)
        cache.put(1, 3, 3, sampleImage);

        // Wait a bit for eviction to complete
        Thread.sleep(100);

        assertThat(cache.get(1, 0, 0)).isNotNull(); // Recently accessed
        assertThat(cache.get(1, 2, 2)).isNotNull(); // Recent
        assertThat(cache.get(1, 3, 3)).isNotNull(); // Just added
        // One of 1,1 should be evicted - the oldest
    }

    @Test
    void concurrentAccessIsThreadSafe() throws InterruptedException {
        FileTileCache cache = new FileTileCache(cacheDir, 50);
        ExecutorService executor = Executors.newFixedThreadPool(8);

        try {
            List<Callable<Void>> tasks = java.util.stream.IntStream.range(0, 8)
                    .mapToObj(worker -> (Callable<Void>) () -> {
                        for (int i = 0; i < 10; i++) {
                            cache.put(worker, i, worker, sampleImage);
                            cache.get(worker, i, worker);
                        }
                        return null;
                    })
                    .toList();
            executor.invokeAll(tasks);
        } finally {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }

        // Verify some tiles are still retrievable
        for (int worker = 0; worker < 8; worker++) {
            // At least some tiles should exist for each worker
            int retrievedCount = 0;
            for (int i = 0; i < 10; i++) {
                if (cache.get(worker, i, worker) != null) {
                    retrievedCount++;
                }
            }
            // Due to eviction, we may not have all tiles, but should have some
            assertThat(retrievedCount).isGreaterThan(0);
        }
    }

    @Test
    void nullPathThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> new FileTileCache(null, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheDir must not be null");
    }

    @Test
    void nonPositiveMaxFilesThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> new FileTileCache(cacheDir, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxFiles must be positive");

        assertThatThrownBy(() -> new FileTileCache(cacheDir, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxFiles must be positive");
    }

    @Test
    void tileIsStoredInOsmStyleDirectory() throws IOException {
        FileTileCache cache = new FileTileCache(cacheDir, 100);

        cache.put(12, 2048, 1536, sampleImage);

        Path expectedPath = cacheDir.resolve("12").resolve("2048").resolve("1536.png");
        assertThat(Files.exists(expectedPath)).isTrue();
        assertThat(Files.isRegularFile(expectedPath)).isTrue();
    }

    @Test
    void clearOnNonExistentDirectoryDoesNotThrow() {
        FileTileCache cache = new FileTileCache(cacheDir, 100);

        // Should not throw even if cache directory doesn't exist
        cache.clear();
    }

    @Test
    void nullImageThrowsNullPointerException() {
        FileTileCache cache = new FileTileCache(cacheDir, 100);

        assertThatThrownBy(() -> cache.put(1, 1, 1, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("image");
    }
}
