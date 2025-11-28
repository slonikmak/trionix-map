package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.trionix.maps.testing.FxTestHarness;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.input.ScrollEvent;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;

class MapViewAnimationTest {

    @Test
    void flyToJumpsImmediatelyWithZeroDuration() {
        FxTestHarness.runOnFxThread(() -> {
            MapView mapView = new MapView();
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(1.0);

            mapView.flyTo(48.8566, 2.3522, 5.0, Duration.ZERO);

            assertThat(mapView.getCenterLat()).isEqualTo(48.8566);
            assertThat(mapView.getCenterLon()).isEqualTo(2.3522);
            assertThat(mapView.getZoom()).isEqualTo(5.0);
        });
    }

    @Test
    void flyToInterpolatesPropertiesAndNotifiesListeners() throws InterruptedException {
        MapView mapView = FxTestHarness.callOnFxThread(MapView::new);
        FxTestHarness.runOnFxThread(() -> {
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(2.0);
        });

        AtomicInteger latUpdates = new AtomicInteger();
        FxTestHarness.runOnFxThread(() ->
                mapView.centerLatProperty().addListener((obs, oldValue, newValue) -> latUpdates.incrementAndGet()));

        FxTestHarness.runOnFxThread(() -> mapView.flyTo(10.0, 15.0, 4.0, Duration.millis(200)));
        Thread.sleep(350);

        double finalLat = FxTestHarness.callOnFxThread(mapView::getCenterLat);
        double finalLon = FxTestHarness.callOnFxThread(mapView::getCenterLon);
        double finalZoom = FxTestHarness.callOnFxThread(mapView::getZoom);

        assertThat(finalLat).isCloseTo(10.0, within(0.05));
        assertThat(finalLon).isCloseTo(15.0, within(0.05));
        assertThat(finalZoom).isCloseTo(4.0, within(0.01));
        assertThat(latUpdates.get()).isGreaterThan(1);
    }

    @Test
    void flyToCancelsWhenUserScrolls() throws InterruptedException {
        MapView mapView = FxTestHarness.callOnFxThread(MapView::new);
        FxTestHarness.runOnFxThread(() -> {
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(2.0);
            mapView.resize(512.0, 512.0);
        });

        FxTestHarness.runOnFxThread(() -> mapView.flyTo(30.0, -90.0, 6.0, Duration.seconds(1)));
        Thread.sleep(150);
        double zoomBeforeScroll = FxTestHarness.callOnFxThread(mapView::getZoom);
        FxTestHarness.runOnFxThread(() -> mapView.fireEvent(createScrollEvent(120.0)));
        Thread.sleep(1200);

        double finalZoom = FxTestHarness.callOnFxThread(mapView::getZoom);
        assertThat(finalZoom).isCloseTo(zoomBeforeScroll + 0.5, within(0.01));
        assertThat(finalZoom).isLessThan(6.0);
    }

    @Test
    void flyToCancelsPreviousAnimationWhenNewOneStarts() throws InterruptedException {
        MapView mapView = FxTestHarness.callOnFxThread(MapView::new);
        FxTestHarness.runOnFxThread(() -> {
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(1.0);
        });

        FxTestHarness.runOnFxThread(() -> mapView.flyTo(20.0, 30.0, 5.0, Duration.seconds(2)));
        Thread.sleep(100);
        FxTestHarness.runOnFxThread(() -> mapView.flyTo(-10.0, -20.0, 3.0, Duration.millis(100)));
        Thread.sleep(300);

        double finalLat = FxTestHarness.callOnFxThread(mapView::getCenterLat);
        double finalLon = FxTestHarness.callOnFxThread(mapView::getCenterLon);
        double finalZoom = FxTestHarness.callOnFxThread(mapView::getZoom);

        assertThat(finalLat).isCloseTo(-10.0, within(0.01));
        assertThat(finalLon).isCloseTo(-20.0, within(0.01));
        assertThat(finalZoom).isCloseTo(3.0, within(0.01));
    }

    @Test
    void scrollZoomAppliesImmediately() throws InterruptedException {
        MapView mapView = FxTestHarness.callOnFxThread(MapView::new);
        FxTestHarness.runOnFxThread(() -> {
            mapView.resize(512.0, 512.0);
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(3.0);
            mapView.getAnimationConfig().setAnimationsEnabled(true);
            mapView.getAnimationConfig().setScrollZoomAnimationEnabled(true);
            mapView.getAnimationConfig().setScrollZoomDuration(Duration.millis(250));
        });

        FxTestHarness.runOnFxThread(() -> mapView.fireEvent(createScrollEvent(120.0)));
        double zoomNow = FxTestHarness.callOnFxThread(mapView::getZoom);
        assertThat(zoomNow).isCloseTo(3.5, within(0.05));
    }

    @Test
    void flyToRespectsGlobalAnimationToggle() {
        MapView mapView = FxTestHarness.callOnFxThread(MapView::new);
        FxTestHarness.runOnFxThread(() -> {
            mapView.resize(512.0, 512.0);
            mapView.setCenterLat(0.0);
            mapView.setCenterLon(0.0);
            mapView.setZoom(2.0);
            mapView.getAnimationConfig().setAnimationsEnabled(false);
        });

        FxTestHarness.runOnFxThread(() -> mapView.flyTo(40.0, -74.0, 5.0, Duration.seconds(1)));

        double finalLat = FxTestHarness.callOnFxThread(mapView::getCenterLat);
        double finalLon = FxTestHarness.callOnFxThread(mapView::getCenterLon);
        double finalZoom = FxTestHarness.callOnFxThread(mapView::getZoom);

        assertThat(finalLat).isEqualTo(40.0);
        assertThat(finalLon).isEqualTo(-74.0);
        assertThat(finalZoom).isEqualTo(5.0);
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
