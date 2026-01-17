package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import javafx.event.EventType;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
class MapViewPinchZoomTest {

    @Start
    private void start(Stage stage) {
        // Initialize JavaFX toolkit
    }

    @Test
    void pinchZoomAppliesImmediately() {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        mapView.resize(512.0, 512.0);
        mapView.setCenterLat(0.0);
        mapView.setCenterLon(0.0);
        mapView.setZoom(3.0);
        mapView.getAnimationConfig().setAnimationsEnabled(true);
        mapView.getAnimationConfig().setTouchZoomAnimationEnabled(true);
        mapView.getAnimationConfig().setPinchGestureAnimationDuration(Duration.millis(250));
        mapView.getAnimationConfig().setPinchMomentumEnabled(false);

        mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM_STARTED, 256.0, 256.0, 1.0));
        mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM, 256.0, 256.0, 1.3));

        double expectedZoom = 3.0 + zoomDelta(1.3);
        assertThat(mapView.getZoom()).isCloseTo(expectedZoom, within(0.05));

        mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM_FINISHED, 256.0, 256.0, 1.0));
    }

    @Test
    void pinchDoesNotApplyMomentumEvenWhenEnabled() throws InterruptedException {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        mapView.resize(512.0, 512.0);
        mapView.setCenterLat(42.0);
        mapView.setCenterLon(-71.0);
        mapView.setZoom(4.0);
        mapView.getAnimationConfig().setAnimationsEnabled(true);
        mapView.getAnimationConfig().setTouchZoomAnimationEnabled(true);
        mapView.getAnimationConfig().setPinchGestureAnimationDuration(Duration.millis(120));
        mapView.getAnimationConfig().setPinchMomentumEnabled(true);
        mapView.getAnimationConfig().setPinchMomentumDuration(Duration.millis(200));

        mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM_STARTED, 300.0, 280.0, 1.0));
        mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM, 300.0, 280.0, 1.4));

        double expectedAfterGesture = 4.0 + zoomDelta(1.4);
        assertThat(mapView.getZoom()).isCloseTo(expectedAfterGesture, within(0.05));

        mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM_FINISHED, 300.0, 280.0, 1.0));

        Thread.sleep(50);
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(mapView.getZoom()).isCloseTo(expectedAfterGesture, within(0.05));
    }

    @Test
    void pinchNoMomentumWhenDisabled() {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        mapView.resize(512.0, 512.0);
        mapView.setCenterLat(10.0);
        mapView.setCenterLon(20.0);
        mapView.setZoom(5.0);
        mapView.getAnimationConfig().setAnimationsEnabled(true);
        mapView.getAnimationConfig().setTouchZoomAnimationEnabled(true);
        mapView.getAnimationConfig().setPinchGestureAnimationDuration(Duration.millis(120));
        mapView.getAnimationConfig().setPinchMomentumEnabled(false);

        mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM_STARTED, 200.0, 200.0, 1.0));
        mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM, 200.0, 200.0, 1.5));

        double expected = 5.0 + zoomDelta(1.5);
        assertThat(mapView.getZoom()).isCloseTo(expected, within(0.05));
    }

    @Test
    void pinchFollowedByScrollCombinesImmediatelyWithoutMomentum() {
        WaitForAsyncUtils.waitForFxEvents();
        MapView mapView = new MapView();
        mapView.resize(512.0, 512.0);
        mapView.setCenterLat(0.0);
        mapView.setCenterLon(0.0);
        mapView.setZoom(3.0);
        mapView.getAnimationConfig().setAnimationsEnabled(true);
        mapView.getAnimationConfig().setTouchZoomAnimationEnabled(true);
        mapView.getAnimationConfig().setPinchGestureAnimationDuration(Duration.millis(120));
        mapView.getAnimationConfig().setPinchMomentumEnabled(true);
        mapView.getAnimationConfig().setPinchMomentumDuration(Duration.millis(300));
        mapView.getAnimationConfig().setScrollZoomAnimationEnabled(false);

        mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM_STARTED, 280.0, 220.0, 1.0));
        mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM, 280.0, 220.0, 1.4));
        mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM_FINISHED, 280.0, 220.0, 1.0));

        mapView.fireEvent(createScrollEvent(120.0, 256.0, 256.0));

        double expected = 3.0 + zoomDelta(1.4) + 0.5;
        assertThat(mapView.getZoom()).isCloseTo(expected, within(0.06));
    }

    private static double zoomDelta(double zoomFactor) {
        return Math.log(zoomFactor) / Math.log(2.0);
    }

    private static ZoomEvent createZoomEvent(EventType<ZoomEvent> type, double x, double y, double zoomFactor) {
        return new ZoomEvent(
                type,
                x,
                y,
                x,
                y,
                false,
                false,
                false,
                false,
                false,
                false,
                zoomFactor,
                zoomFactor,
                new PickResult(null, x, y));
    }

    private static ScrollEvent createScrollEvent(double deltaY, double x, double y) {
        return new ScrollEvent(
                ScrollEvent.SCROLL,
                x,
                y,
                x,
                y,
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
