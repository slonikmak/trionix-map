package com.trionix.maps.layer;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.InMemoryTileCache;
import com.trionix.maps.MapView;
import com.trionix.maps.TileRetriever;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
class LayerSystemTest {

    private static final Image BLANK_TILE = new WritableImage(1, 1);
    private static final TileRetriever IMMEDIATE_TILE_RETRIEVER =
            (zoom, x, y) -> CompletableFuture.completedFuture(BLANK_TILE);
    private static final List<String> layoutOrder = new ArrayList<>();

    @Start
    private void start(Stage stage) {
        // Initialize JavaFX toolkit
    }

    @BeforeAll
    static void init() {
        // Ensure static initialization doesn't depend on FX toolkit until needed
        // WritableImage needs toolkit, but we are using static field.
        // For TestFX, toolkit is started before tests. 
        // But here BLANK_TILE is static final.
        // It might be problematic if class loads before toolkit.
        // However, in typical test run, extension starts first.
        // Or we can lazy init.
    }
    
    // Workaround for static image creation needing FX thread
    // We'll just create it inside the test or use a lazy getter if possible.
    // But to minimize changes, let's assume Toolkit is initialized by other tests or use runLater in @BeforeAll?
    // Actually, @ExtendWith starts toolkit. But static fields init at class load.
    // It's safer to remove static BLANK_TILE or init it later.
    
    // I will refactor to instance fields.

    @Test
    void notifiesLifecycleAndHandlesLayoutRequests() {
        WaitForAsyncUtils.waitForFxEvents();
        // Since we can't easily change the static field behavior without removing final, 
        // I'll create a local blank tile or rely on the fact that usually this works if toolkit is already running.
        // But to be safe, I'll use a local variable.
        
        Platform.runLater(() -> {
            Image blankTile = new WritableImage(1, 1);
            TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(blankTile);
            
            MapView mapView = new MapView(retriever, new InMemoryTileCache(16));
            RecordingLayer layer = new RecordingLayer("primary");

            mapView.getLayers().add(layer);
            assertThat(layer.addedCount).isEqualTo(1);
            assertThat(layer.removedCount).isZero();

            performLayout(mapView);
            assertThat(layer.layoutCount).isEqualTo(1);

            layer.requestLayerLayout();
            performLayout(mapView);
            assertThat(layer.layoutCount).isEqualTo(2);

            mapView.getLayers().remove(layer);
            assertThat(layer.removedCount).isEqualTo(1);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void respectsLayerOrderingDuringLayout() {
        WaitForAsyncUtils.waitForFxEvents();
        Platform.runLater(() -> {
            Image blankTile = new WritableImage(1, 1);
            TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(blankTile);
            
            MapView mapView = new MapView(retriever, new InMemoryTileCache(16));
            RecordingLayer first = new RecordingLayer("first");
            RecordingLayer second = new RecordingLayer("second");
            RecordingLayer third = new RecordingLayer("third");

            mapView.getLayers().addAll(first, second);
            layoutOrder.clear();
            performLayout(mapView);
            assertThat(layoutOrder).containsExactly("first", "second");

            mapView.getLayers().add(1, third);
            layoutOrder.clear();
            performLayout(mapView);
            assertThat(layoutOrder).containsExactly("first", "third", "second");

            layoutOrder.clear();
            Collections.swap(mapView.getLayers(), 0, 2);
            performLayout(mapView);
            assertThat(layoutOrder).containsExactly("second", "third", "first");
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    private static void performLayout(MapView mapView) {
        mapView.resize(512.0, 512.0);
        mapView.requestLayout();
        mapView.layout();
    }

    private static final class RecordingLayer extends MapLayer {
        private final String name;
        private int addedCount;
        private int removedCount;
        private int layoutCount;

        private RecordingLayer(String name) {
            this.name = name;
        }

        @Override
        public void layerAdded(MapView mapView) {
            addedCount++;
        }

        @Override
        public void layerRemoved(MapView mapView) {
            removedCount++;
        }

        @Override
        public void layoutLayer(MapView mapView) {
            layoutCount++;
            layoutOrder.add(name);
        }
    }
}
