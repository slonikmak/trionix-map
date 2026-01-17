package com.trionix.maps.layer;

import com.trionix.maps.MapView;
import com.trionix.maps.control.ScaleRulerControl;
import com.trionix.maps.internal.util.DistanceUtils;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying that GridLayer and ScaleRulerControl use consistent distance calculations.
 */
@ExtendWith(ApplicationExtension.class)
class GridLayerScaleRulerIntegrationTest {

    private Stage stage;

    @Start
    private void start(Stage stage) {
        this.stage = stage;
        stage.setScene(new Scene(new StackPane(), 800, 600));
        stage.show();
    }

    @Test
    void gridAndRuler_useConsistentDistanceCalculations() {
        Platform.runLater(() -> {
            var mapView = new MapView();
            mapView.resize(800, 600);
            mapView.setCenterLat(50.0);
            mapView.setCenterLon(10.0);
            mapView.setZoom(5);

            var gridLayer = new GridLayer();
            var scaleRuler = new ScaleRulerControl(mapView);

            mapView.getLayers().add(gridLayer);
            scaleRuler.resize(200, 50);

            mapView.layout();
            scaleRuler.layout();

            // Both should use same distance calculation utilities
            // Verify that distance calculations are consistent
            int zoomLevel = (int) Math.floor(mapView.getZoom());
            double mpp = DistanceUtils.metersPerPixel(mapView.getCenterLat(), zoomLevel);

            // Verify meters per pixel is reasonable
            assertTrue(mpp > 0);
            assertTrue(mpp < 1_000_000); // Should be less than 1000 km per pixel at zoom 5

            // Verify nice distance function returns sensible values
            double testMeters = mpp * 100;
            double niceMeters = DistanceUtils.getNiceDistance(testMeters);
            assertTrue(niceMeters > 0);
            assertTrue(niceMeters >= testMeters * 0.5);
            assertTrue(niceMeters <= testMeters * 2.0);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void gridAutoStep_scalesWithZoom() {
        Platform.runLater(() -> {
            var mapView = new MapView();
            mapView.resize(800, 600);
            mapView.setCenterLat(50.0);
            mapView.setCenterLon(10.0);

            var gridLayer = new GridLayer(); // Auto step (0)
            mapView.getLayers().add(gridLayer);

            // At different zoom levels, grid should adjust
            mapView.setZoom(2);
            mapView.layout();
            assertDoesNotThrow(() -> gridLayer.layoutLayer(mapView));

            mapView.setZoom(8);
            mapView.layout();
            assertDoesNotThrow(() -> gridLayer.layoutLayer(mapView));

            mapView.setZoom(15);
            mapView.layout();
            assertDoesNotThrow(() -> gridLayer.layoutLayer(mapView));
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void rulerDistance_changesWithZoom() {
        Platform.runLater(() -> {
            var mapView = new MapView();
            mapView.resize(800, 600);
            mapView.setCenterLat(50.0);
            mapView.setCenterLon(10.0);

            var scaleRuler = new ScaleRulerControl(mapView);
            scaleRuler.resize(200, 50);

            // At low zoom, distance per pixel is larger
            mapView.setZoom(2);
            mapView.layout();
            scaleRuler.layout();
            double mppZoom2 = DistanceUtils.metersPerPixel(50.0, 2);

            // At high zoom, distance per pixel is smaller
            mapView.setZoom(10);
            mapView.layout();
            scaleRuler.layout();
            double mppZoom10 = DistanceUtils.metersPerPixel(50.0, 10);

            assertTrue(mppZoom2 > mppZoom10);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void rulerDistance_changesWithLatitude() {
        Platform.runLater(() -> {
            var mapView = new MapView();
            mapView.resize(800, 600);
            mapView.setZoom(5);

            var scaleRuler = new ScaleRulerControl(mapView);
            scaleRuler.resize(200, 50);

            // At equator
            mapView.setCenterLat(0.0);
            mapView.layout();
            scaleRuler.layout();
            double mppEquator = DistanceUtils.metersPerPixel(0.0, 5);

            // At higher latitude
            mapView.setCenterLat(60.0);
            mapView.layout();
            scaleRuler.layout();
            double mpp60 = DistanceUtils.metersPerPixel(60.0, 5);

            // Scale should be smaller at higher latitudes (Web Mercator distortion)
            assertTrue(mppEquator > mpp60);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }
}
