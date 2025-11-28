package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.trionix.maps.testing.FxTestHarness;
import javafx.event.EventType;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;

class MapViewPinchZoomTest {

    @Test
    void pinchZoomAppliesImmediately() throws InterruptedException {
        MapView mapView = FxTestHarness.callOnFxThread(MapView::new);
        FxTestHarness.runOnFxThread(() -> {
            mapView.resize(512.0, 512.0);
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(3.0);
            mapView.getAnimationConfig().setAnimationsEnabled(true);
            mapView.getAnimationConfig().setTouchZoomAnimationEnabled(true);
            mapView.getAnimationConfig().setPinchGestureAnimationDuration(Duration.millis(250));
            mapView.getAnimationConfig().setPinchMomentumEnabled(false);
        });

        FxTestHarness.runOnFxThread(() -> {
            mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM_STARTED, 256.0, 256.0, 1.0));
            mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM, 256.0, 256.0, 1.3));
        });

        // Zooms now apply immediately for pinch input — assert the change took effect right away
        double expectedZoom = 3.0 + zoomDelta(1.3);
        double zoomNow = FxTestHarness.callOnFxThread(mapView::getZoom);
        assertThat(zoomNow).isCloseTo(expectedZoom, within(0.05));

        FxTestHarness.runOnFxThread(() ->
                mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM_FINISHED, 256.0, 256.0, 1.0)));
    }

    @Test
    void pinchDoesNotApplyMomentumEvenWhenEnabled() throws InterruptedException {
        MapView mapView = FxTestHarness.callOnFxThread(MapView::new);
        FxTestHarness.runOnFxThread(() -> {
            mapView.resize(512.0, 512.0);
            mapView.setCenterLat(42.0);
            mapView.setCenterLon(-71.0);
            mapView.setZoom(4.0);
            mapView.getAnimationConfig().setAnimationsEnabled(true);
            mapView.getAnimationConfig().setTouchZoomAnimationEnabled(true);
            mapView.getAnimationConfig().setPinchGestureAnimationDuration(Duration.millis(120));
            mapView.getAnimationConfig().setPinchMomentumEnabled(true);
            mapView.getAnimationConfig().setPinchMomentumDuration(Duration.millis(200));
        });

        FxTestHarness.runOnFxThread(() -> {
            mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM_STARTED, 300.0, 280.0, 1.0));
            mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM, 300.0, 280.0, 1.4));
        });

        // The zoom will apply immediately and no momentum should be added after finish
        double expectedAfterGesture = 4.0 + zoomDelta(1.4);
        double zoomAfter = FxTestHarness.callOnFxThread(mapView::getZoom);
        assertThat(zoomAfter).isCloseTo(expectedAfterGesture, within(0.05));

        FxTestHarness.runOnFxThread(() ->
                mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM_FINISHED, 300.0, 280.0, 1.0)));

        // Confirm no extra momentum applied
        Thread.sleep(50);
        double zoomFinal = FxTestHarness.callOnFxThread(mapView::getZoom);
        assertThat(zoomFinal).isCloseTo(expectedAfterGesture, within(0.05));
    }

    @Test
    void pinchNoMomentumWhenDisabled() throws InterruptedException {
        MapView mapView = FxTestHarness.callOnFxThread(MapView::new);
        FxTestHarness.runOnFxThread(() -> {
            mapView.resize(512.0, 512.0);
            mapView.setCenterLat(10.0);
            mapView.setCenterLon(20.0);
            mapView.setZoom(5.0);
            mapView.getAnimationConfig().setAnimationsEnabled(true);
            mapView.getAnimationConfig().setTouchZoomAnimationEnabled(true);
            mapView.getAnimationConfig().setPinchGestureAnimationDuration(Duration.millis(120));
            mapView.getAnimationConfig().setPinchMomentumEnabled(false);
        });

        FxTestHarness.runOnFxThread(() -> {
            mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM_STARTED, 200.0, 200.0, 1.0));
            mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM, 200.0, 200.0, 1.5));
        });

        // Zoom applied immediately, no momentum expected
        double zoomAfter = FxTestHarness.callOnFxThread(mapView::getZoom);
        double expected = 5.0 + zoomDelta(1.5);
        assertThat(zoomAfter).isCloseTo(expected, within(0.05));
    }

    @Test
    void pinchFollowedByScrollCombinesImmediatelyWithoutMomentum() throws InterruptedException {
        MapView mapView = FxTestHarness.callOnFxThread(MapView::new);
        FxTestHarness.runOnFxThread(() -> {
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
        });

        FxTestHarness.runOnFxThread(() -> {
            mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM_STARTED, 280.0, 220.0, 1.0));
            mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM, 280.0, 220.0, 1.4));
            mapView.fireEvent(createZoomEvent(ZoomEvent.ZOOM_FINISHED, 280.0, 220.0, 1.0));
        });

        // Immediately fire a scroll - both effects apply instantly
        FxTestHarness.runOnFxThread(() -> mapView.fireEvent(createScrollEvent(120.0, 256.0, 256.0)));

        double expected = 3.0 + zoomDelta(1.4) + 0.5; // scrollStep = +0.5
        double zoomAfter = FxTestHarness.callOnFxThread(mapView::getZoom);
        assertThat(zoomAfter).isCloseTo(expected, within(0.06));
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
