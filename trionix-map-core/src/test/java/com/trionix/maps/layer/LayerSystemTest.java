package com.trionix.maps.layer;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.InMemoryTileCache;
import com.trionix.maps.MapView;
import com.trionix.maps.TileRetriever;
import com.trionix.maps.testing.FxTestHarness;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;

class LayerSystemTest {

    private static final Image BLANK_TILE = new WritableImage(1, 1);
    private static final TileRetriever IMMEDIATE_TILE_RETRIEVER =
            (zoom, x, y) -> CompletableFuture.completedFuture(BLANK_TILE);
    private static final List<String> layoutOrder = new ArrayList<>();

    @Test
    void notifiesLifecycleAndHandlesLayoutRequests() {
        FxTestHarness.runOnFxThread(() -> {
            MapView mapView = new MapView(IMMEDIATE_TILE_RETRIEVER, new InMemoryTileCache(16));
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
    }

    @Test
    void respectsLayerOrderingDuringLayout() {
        FxTestHarness.runOnFxThread(() -> {
            MapView mapView = new MapView(IMMEDIATE_TILE_RETRIEVER, new InMemoryTileCache(16));
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
