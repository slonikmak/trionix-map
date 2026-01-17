package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
class FileTileCacheTest {

    private Image sampleImage;

    @TempDir
    Path tempDir;

    private Path cacheDir;

    @Start
    private void start(Stage stage) {
        // Initialize JavaFX toolkit
    }

    @BeforeEach
    void setUp() {
        WaitForAsyncUtils.waitForFxEvents();
        sampleImage = new WritableImage(256, 256);
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

        cache.put(1, 0, 0, sampleImage);
        Thread.sleep(50);
        cache.put(1, 1, 1, sampleImage);
        Thread.sleep(50);
        cache.put(1, 2, 2, sampleImage);
        Thread.sleep(50);

        cache.get(1, 0, 0);
        Thread.sleep(50);

        cache.put(1, 3, 3, sampleImage);

        Thread.sleep(100);

        assertThat(cache.get(1, 0, 0)).isNotNull();
        assertThat(cache.get(1, 2, 2)).isNotNull();
        assertThat(cache.get(1, 3, 3)).isNotNull();
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

        for (int worker = 0; worker < 8; worker++) {
            int retrievedCount = 0;
            for (int i = 0; i < 10; i++) {
                if (cache.get(worker, i, worker) != null) {
                    retrievedCount++;
                }
            }
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

        cache.clear();
    }

    @Test
    void nullImageThrowsNullPointerException() {
        FileTileCache cache = new FileTileCache(cacheDir, 100);

        assertThatThrownBy(() -> cache.put(1, 1, 1, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("image");
    }

    @Test
    void get_returnsNullWhenFileDoesNotExist() {
        FileTileCache cache = new FileTileCache(cacheDir, 100);

        Image result = cache.get(1, 999, 999);

        assertThat(result).isNull();
    }

    @Test
    void get_returnsNullWhenFileDeletedConcurrently() throws IOException {
        FileTileCache cache = new FileTileCache(cacheDir, 100);

        cache.put(1, 1, 1, sampleImage);
        Path tilePath = cacheDir.resolve("1").resolve("1").resolve("1.png");
        assertThat(Files.exists(tilePath)).isTrue();

        Files.delete(tilePath);

        Image result = cache.get(1, 1, 1);

        assertThat(result).isNull();
    }
}
