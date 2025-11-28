package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.testing.FxTestHarness;
import com.trionix.maps.testing.MapViewTestHarness;
import com.trionix.maps.testing.MapViewTestHarness.MountedMapView;
import com.trionix.maps.testing.RecordingTileRetriever;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TieredTileCacheIntegrationTest {

    private static Image sampleImage;

    @TempDir
    Path tempDir;

    private Path cacheDir;

    @BeforeAll
    static void setupImage() {
        sampleImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
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
    void mapViewWorksWithTieredCache() throws Exception {
        RecordingTileRetriever retriever = new RecordingTileRetriever();
        TileCache tieredCache = new TieredTileCache(List.of(
                new InMemoryTileCache(100),
                new FileTileCache(cacheDir, 500)
        ));

        try (MountedMapView mounted = MapViewTestHarness.mount(
                () -> new MapView(retriever, tieredCache),
                256,
                256)) {
            mounted.layout();

            // Wait for tile requests
            var requests = retriever.awaitRequests(1, Duration.ofSeconds(5));
            assertThat(requests).isNotEmpty();

            // Complete the requests
            requests.forEach(request -> request.future().complete(sampleImage));
            mounted.flushFx();
        }
    }

    @Test
    void mapViewWorksWithBuilderCreatedCache() throws Exception {
        RecordingTileRetriever retriever = new RecordingTileRetriever();
        TileCache cache = TileCacheBuilder.create()
                .memory(100)
                .disk(cacheDir, 500)
                .build();

        try (MountedMapView mounted = MapViewTestHarness.mount(
                () -> new MapView(retriever, cache),
                256,
                256)) {
            mounted.layout();

            // Wait for tile requests
            var requests = retriever.awaitRequests(1, Duration.ofSeconds(5));
            assertThat(requests).isNotEmpty();

            // Complete the requests
            requests.forEach(request -> request.future().complete(sampleImage));
            mounted.flushFx();
        }
    }

    @Test
    void tilesPersistAcrossL1ClearWhenL2IsDisk() {
        var l1 = new InMemoryTileCache(100);
        var l2 = new FileTileCache(cacheDir, 500);
        var tiered = new TieredTileCache(List.of(l1, l2));

        // Put a tile
        tiered.put(5, 10, 15, sampleImage);

        // Verify both tiers have it
        assertThat(l1.get(5, 10, 15)).isNotNull();
        assertThat(l2.get(5, 10, 15)).isNotNull();

        // Clear L1 only
        l1.clear();

        // L1 should be empty, L2 should still have it
        assertThat(l1.get(5, 10, 15)).isNull();
        assertThat(l2.get(5, 10, 15)).isNotNull();

        // Tiered cache should still return the tile (from L2)
        Image retrieved = tiered.get(5, 10, 15);
        assertThat(retrieved).isNotNull();

        // After retrieval, L1 should be repopulated (promotion)
        assertThat(l1.get(5, 10, 15)).isNotNull();
    }

    @Test
    void diskCachePersistsTilesBetweenInstances() {
        // First instance writes tiles
        var firstCache = new FileTileCache(cacheDir, 500);
        firstCache.put(5, 10, 15, sampleImage);
        firstCache.put(6, 20, 25, sampleImage);

        // Second instance reads the same files
        var secondCache = new FileTileCache(cacheDir, 500);
        assertThat(secondCache.get(5, 10, 15)).isNotNull();
        assertThat(secondCache.get(6, 20, 25)).isNotNull();
    }
}
