package com.trionix.maps.layer;

import com.trionix.maps.MapView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class GridLayerTest {

    private Stage stage;

    @Start
    private void start(Stage stage) {
        this.stage = stage;
        stage.setScene(new Scene(new StackPane(), 800, 600));
        stage.show();
    }

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
        Platform.runLater(() -> {
            MapView mapView = new MapView();
            ((StackPane) stage.getScene().getRoot()).getChildren().setAll(mapView);
            // Resize to 0
            mapView.resize(0.0, 0.0);
            
            GridLayer layer = new GridLayer();
            mapView.getLayers().add(layer);
            
            mapView.requestLayout();
            mapView.layout();

            assertDoesNotThrow(() -> layer.layoutLayer(mapView));
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void layoutLayer_withValidSize_doesNotThrow() {
        Platform.runLater(() -> {
            MapView mapView = new MapView();
            ((StackPane) stage.getScene().getRoot()).getChildren().setAll(mapView);
            mapView.resize(800.0, 600.0);
            
            GridLayer layer = new GridLayer();
            mapView.getLayers().add(layer);
            mapView.setCenterLat(50.0);
            mapView.setCenterLon(10.0);
            mapView.setZoom(5);

            mapView.requestLayout();
            mapView.layout();

            assertDoesNotThrow(() -> layer.layoutLayer(mapView));
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void visibilityToggle_affectsRendering() {
        Platform.runLater(() -> {
            MapView mapView = new MapView();
            ((StackPane) stage.getScene().getRoot()).getChildren().setAll(mapView);
            mapView.resize(800.0, 600.0);

            GridLayer layer = new GridLayer();
            mapView.getLayers().add(layer);

            mapView.requestLayout();
            mapView.layout();

            assertTrue(layer.isVisible(), "Layer should be visible by default");

            layer.setVisible(false);
            assertFalse(layer.isVisible(), "Layer should be hidden after setVisible(false)");

            layer.setVisible(true);
            assertTrue(layer.isVisible(), "Layer should be visible after setVisible(true)");
        });
        WaitForAsyncUtils.waitForFxEvents();
    }
}
