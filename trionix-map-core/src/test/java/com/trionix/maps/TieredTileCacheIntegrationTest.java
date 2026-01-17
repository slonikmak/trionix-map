package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.testing.RecordingTileRetriever;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
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
class TieredTileCacheIntegrationTest {

    private Image sampleImage;
    private Stage stage;

    @TempDir
    Path tempDir;

    private Path cacheDir;

    @Start
    private void start(Stage stage) {
        this.stage = stage;
        stage.setScene(new Scene(new StackPane(), 256, 256));
        stage.show();
    }

    @BeforeEach
    void setUp() {
        WaitForAsyncUtils.waitForFxEvents();
        sampleImage = new WritableImage(256, 256);
        cacheDir = tempDir.resolve("tiles");
    }

    @AfterEach
    void tearDown() throws IOException {
        Platform.runLater(() -> {
            if (stage != null && stage.getScene() != null) {
                ((StackPane) stage.getScene().getRoot()).getChildren().clear();
            }
        });
        WaitForAsyncUtils.waitForFxEvents();

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

        mountMapView(retriever, tieredCache);

        // Wait for tile requests
        var requests = retriever.awaitRequests(1, Duration.ofSeconds(5));
        assertThat(requests).isNotEmpty();

        // Complete the requests
        requests.forEach(request -> request.future().complete(sampleImage));
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void mapViewWorksWithBuilderCreatedCache() throws Exception {
        RecordingTileRetriever retriever = new RecordingTileRetriever();
        TileCache cache = TileCacheBuilder.create()
                .memory(100)
                .disk(cacheDir, 500)
                .build();

        mountMapView(retriever, cache);

        // Wait for tile requests
        var requests = retriever.awaitRequests(1, Duration.ofSeconds(5));
        assertThat(requests).isNotEmpty();

        // Complete the requests
        requests.forEach(request -> request.future().complete(sampleImage));
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void tilesPersistAcrossL1ClearWhenL2IsDisk() {
        var l1 = new InMemoryTileCache(100);
        var l2 = new FileTileCache(cacheDir, 500);
        var tiered = new TieredTileCache(List.of(l1, l2));

        tiered.put(5, 10, 15, sampleImage);

        assertThat(l1.get(5, 10, 15)).isNotNull();
        assertThat(l2.get(5, 10, 15)).isNotNull();

        l1.clear();

        assertThat(l1.get(5, 10, 15)).isNull();
        assertThat(l2.get(5, 10, 15)).isNotNull();

        Image retrieved = tiered.get(5, 10, 15);
        assertThat(retrieved).isNotNull();

        assertThat(l1.get(5, 10, 15)).isNotNull();
    }

    @Test
    void diskCachePersistsTilesBetweenInstances() {
        var firstCache = new FileTileCache(cacheDir, 500);
        firstCache.put(5, 10, 15, sampleImage);
        firstCache.put(6, 20, 25, sampleImage);

        var secondCache = new FileTileCache(cacheDir, 500);
        assertThat(secondCache.get(5, 10, 15)).isNotNull();
        assertThat(secondCache.get(6, 20, 25)).isNotNull();
    }

    private void mountMapView(TileRetriever retriever, TileCache cache) {
        Platform.runLater(() -> {
            MapView mapView = new MapView(retriever, cache);
            StackPane root = (StackPane) stage.getScene().getRoot();
            root.getChildren().setAll(mapView);
            mapView.resize(256, 256);
            mapView.requestLayout();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }
}
