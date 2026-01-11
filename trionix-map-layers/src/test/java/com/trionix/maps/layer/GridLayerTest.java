package com.trionix.maps.layer;

import com.trionix.maps.MapView;
import com.trionix.maps.testing.FxTestHarness;
import com.trionix.maps.testing.MapViewTestHarness;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridLayerTest {

    @Test
    void constructor_setsDefaults() {
        var layer = new GridLayer();

        assertNotNull(layer.getStrokeColor(), "Default stroke color should not be null");
        assertEquals(1.0, layer.getStrokeWidth(), "Default stroke width should be 1.0");
    }

    @Test
    void setStrokeColor_updatesProperty() {
        var layer = new GridLayer();
        layer.setStrokeColor(Color.RED);
        assertEquals(Color.RED, layer.getStrokeColor());
    }

    @Test
    void setStrokeWidth_updatesProperty() {
        var layer = new GridLayer();
        layer.setStrokeWidth(2.5);
        assertEquals(2.5, layer.getStrokeWidth());
    }

    @Test
    void layoutLayer_withZeroSize_doesNotThrow() {
        try (var mounted = MapViewTestHarness.mount(MapView::new, 0.0, 0.0)) {
            MapView mapView = mounted.mapView();
            GridLayer layer = new GridLayer();

            FxTestHarness.runOnFxThread(() -> {
                mapView.getLayers().add(layer);
            });

            mounted.layout();

            // Should not throw
            FxTestHarness.runOnFxThread(() -> {
                assertDoesNotThrow(() -> layer.layoutLayer(mapView));
            });
        }
    }

    @Test
    void layoutLayer_withValidSize_doesNotThrow() {
        try (var mounted = MapViewTestHarness.mount(MapView::new, 800.0, 600.0)) {
            MapView mapView = mounted.mapView();
            GridLayer layer = new GridLayer();

            FxTestHarness.runOnFxThread(() -> {
                mapView.getLayers().add(layer);
                mapView.setCenterLat(50.0);
                mapView.setCenterLon(10.0);
                mapView.setZoom(5);
            });

            mounted.layout();

            FxTestHarness.runOnFxThread(() -> {
                assertDoesNotThrow(() -> layer.layoutLayer(mapView));
            });
        }
    }

    @Test
    void visibilityToggle_affectsRendering() {
        try (var mounted = MapViewTestHarness.mount(MapView::new, 800.0, 600.0)) {
            MapView mapView = mounted.mapView();
            GridLayer layer = new GridLayer();

            FxTestHarness.runOnFxThread(() -> {
                mapView.getLayers().add(layer);
            });

            mounted.layout();

            FxTestHarness.runOnFxThread(() -> {
                assertTrue(layer.isVisible(), "Layer should be visible by default");

                layer.setVisible(false);
                assertFalse(layer.isVisible(), "Layer should be hidden after setVisible(false)");

                layer.setVisible(true);
                assertTrue(layer.isVisible(), "Layer should be visible after setVisible(true)");
            });
        }
    }
}
