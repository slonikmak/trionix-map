package com.trionix.maps;

import com.trionix.maps.internal.tiles.TileCoordinate;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class MapViewRegressionTest extends ApplicationTest {

    private MapView mapView;
    private MockTileRetriever mockRetriever;

    @Override
    public void start(Stage stage) {
        mockRetriever = new MockTileRetriever();
        // Use a small cache to force retrievals if needed
        TileCache cache = new InMemoryTileCache(10);
        mapView = new MapView(mockRetriever, cache);
        mapView.setPrefSize(800, 600);

        StackPane root = new StackPane(mapView);
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();
        // Wait for layout to occur
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    public void testMapLoadsAndRendersTiles() throws InterruptedException {
        // Initial state
        interact(() -> {
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(2.0); // Low zoom to ensure few tiles
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Wait for tiles to be requested
        assertTrue(mockRetriever.awaitLoad(5, TimeUnit.SECONDS), "Tiles should be requested via retriever");
        assertTrue(mockRetriever.loadCount.get() > 0, "Should have attempted to load tiles");

        // Verify Canvas exists in the scene graph
        Node canvasNode = lookup(node -> node instanceof Canvas).query();
        assertNotNull(canvasNode, "Canvas should exist in the scene graph (used for rendering tiles)");
        assertTrue(canvasNode.isVisible(), "Canvas should be visible");
    }

    @Test
    public void testZoomTriggersNewTileLoads() throws InterruptedException {
        // Wait for initial stable state
        assertTrue(mockRetriever.awaitLoad(5, TimeUnit.SECONDS));

        // Reset latch inside interact block to ensure it happens before the scheduled
        // refresh
        interact(() -> {
            mockRetriever.resetLatch();
            mapView.setZoom(3.0);
        });

        // Changing zoom should trigger new loads
        assertTrue(mockRetriever.awaitLoad(5, TimeUnit.SECONDS), "New tiles should be requested after zoom");
    }

    // A mock retriever to verify calls
    static class MockTileRetriever implements TileRetriever {
        final AtomicInteger loadCount = new AtomicInteger(0);
        private volatile CountDownLatch latch = new CountDownLatch(1);

        @Override
        public CompletableFuture<Image> loadTile(int zoom, long x, long y) {
            loadCount.incrementAndGet();
            latch.countDown();
            // Return a dummy image
            WritableImage image = new WritableImage(256, 256);
            return CompletableFuture.completedFuture(image);
        }

        boolean awaitLoad(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        void resetLatch() {
            latch = new CountDownLatch(1);
        }
    }
}
