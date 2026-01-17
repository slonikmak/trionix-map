package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
class MapViewAnimationTest {

    @Start
    private void start(Stage stage) {
        // Initialize JavaFX toolkit
    }

    @Test
    void flyToJumpsImmediatelyWithZeroDuration() {
        WaitForAsyncUtils.waitForFxEvents();
        
        MapView mapView = new MapView();
        Platform.runLater(() -> {
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(1.0);
            mapView.flyTo(48.8566, 2.3522, 5.0, Duration.ZERO);
        });
        
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(mapView.getCenterLat()).isEqualTo(48.8566);
        assertThat(mapView.getCenterLon()).isEqualTo(2.3522);
        assertThat(mapView.getZoom()).isEqualTo(5.0);
    }

    @Test
    void flyToInterpolatesPropertiesAndNotifiesListeners() throws InterruptedException {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        
        Platform.runLater(() -> {
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(2.0);
        });
        WaitForAsyncUtils.waitForFxEvents();

        AtomicInteger latUpdates = new AtomicInteger();
        Platform.runLater(() -> mapView.centerLatProperty().addListener((obs, oldValue, newValue) -> latUpdates.incrementAndGet()));

        Platform.runLater(() -> mapView.flyTo(10.0, 15.0, 4.0, Duration.millis(200)));
        
        // Wait longer than animation
        Thread.sleep(350);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(mapView.getCenterLat()).isCloseTo(10.0, within(0.05));
        assertThat(mapView.getCenterLon()).isCloseTo(15.0, within(0.05));
        assertThat(mapView.getZoom()).isCloseTo(4.0, within(0.01));
        assertThat(latUpdates.get()).isGreaterThan(1);
    }

    @Test
    void flyToCancelsWhenUserScrolls() throws InterruptedException {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        Platform.runLater(() -> {
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(2.0);
            mapView.resize(512.0, 512.0);
            mapView.flyTo(30.0, -90.0, 6.0, Duration.seconds(1));
        });
        WaitForAsyncUtils.waitForFxEvents();
        
        Thread.sleep(150);
        double zoomBeforeScroll = mapView.getZoom();
        
        Platform.runLater(() -> mapView.fireEvent(createScrollEvent(120.0)));
        Thread.sleep(1200);
        WaitForAsyncUtils.waitForFxEvents();

        double finalZoom = mapView.getZoom();
        assertThat(finalZoom).isCloseTo(zoomBeforeScroll + 0.5, within(0.01));
        assertThat(finalZoom).isLessThan(6.0);
    }

    @Test
    void flyToCancelsPreviousAnimationWhenNewOneStarts() throws InterruptedException {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        Platform.runLater(() -> {
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(1.0);
            mapView.flyTo(20.0, 30.0, 5.0, Duration.seconds(2));
        });
        WaitForAsyncUtils.waitForFxEvents();
        
        Thread.sleep(100);
        Platform.runLater(() -> mapView.flyTo(-10.0, -20.0, 3.0, Duration.millis(100)));
        Thread.sleep(300);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(mapView.getCenterLat()).isCloseTo(-10.0, within(0.01));
        assertThat(mapView.getCenterLon()).isCloseTo(-20.0, within(0.01));
        assertThat(mapView.getZoom()).isCloseTo(3.0, within(0.01));
    }

    @Test
    void scrollZoomAppliesImmediately() {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        Platform.runLater(() -> {
            mapView.resize(512.0, 512.0);
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(3.0);
            mapView.getAnimationConfig().setAnimationsEnabled(true);
            mapView.getAnimationConfig().setScrollZoomAnimationEnabled(true);
            mapView.getAnimationConfig().setScrollZoomDuration(Duration.millis(250));
            mapView.fireEvent(createScrollEvent(120.0));
        });
        
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(mapView.getZoom()).isCloseTo(3.5, within(0.05));
    }

    @Test
    void flyToRespectsGlobalAnimationToggle() {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        Platform.runLater(() -> {
            mapView.resize(512.0, 512.0);
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(2.0);
            mapView.getAnimationConfig().setAnimationsEnabled(false);
            mapView.flyTo(40.0, -74.0, 5.0, Duration.seconds(1));
        });
        
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(mapView.getCenterLat()).isEqualTo(40.0);
        assertThat(mapView.getCenterLon()).isEqualTo(-74.0);
        assertThat(mapView.getZoom()).isEqualTo(5.0);
    }

    private static ScrollEvent createScrollEvent(double deltaY) {
        return new ScrollEvent(
                ScrollEvent.SCROLL,
                0.0,
                0.0,
                0.0,
                0.0,
                false,
                false,
                false,
                false,
                false,
                false,
                0.0,
                deltaY,
                0.0,
                deltaY,
                ScrollEvent.HorizontalTextScrollUnits.NONE,
                0.0,
                ScrollEvent.VerticalTextScrollUnits.NONE,
                0.0,
                0,
                null);
    }
}
