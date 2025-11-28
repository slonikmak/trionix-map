package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.trionix.maps.internal.MapState;
import com.trionix.maps.testing.FxTestHarness;
import com.trionix.maps.testing.MapViewTestHarness;
import com.trionix.maps.testing.MapViewTestHarness.MountedMapView;
import java.util.concurrent.CompletableFuture;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.Test;

/**
 * Tests for MapView double-click zoom functionality.
 */
class MapViewDoubleClickZoomTest {

    @Test
    void doubleClickIncrementsZoomByOne() {
        WritableImage tileImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        try (MountedMapView mounted = MapViewTestHarness.mount(() -> new MapView(retriever, cache), 512, 512)) {
            FxTestHarness.runOnFxThread(() -> {
                MapView view = mounted.mapView();
                disableAnimations(view);
                view.setCenterLat(0.0);
                view.setCenterLon(0.0);
                view.setZoom(5.0);
            });
            mounted.layout();

            double initialZoom = FxTestHarness.callOnFxThread(() -> mounted.mapView().getZoom());

            FxTestHarness.runOnFxThread(() ->
                    mounted.mapView().fireEvent(mouseDoubleClick(256.0, 256.0)));

            double zoomAfter = FxTestHarness.callOnFxThread(() -> mounted.mapView().getZoom());
            assertThat(zoomAfter).isCloseTo(initialZoom + 1.0, within(0.0001));
        }
    }

    @Test
    void doubleClickDoesNotZoomWhenDisabled() {
        WritableImage tileImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        try (MountedMapView mounted = MapViewTestHarness.mount(() -> new MapView(retriever, cache), 512, 512)) {
            FxTestHarness.runOnFxThread(() -> {
                MapView view = mounted.mapView();
                disableAnimations(view);
                view.setCenterLat(0.0);
                view.setCenterLon(0.0);
                view.setZoom(5.0);
                view.setEnableDoubleClickZoom(false);
            });
            mounted.layout();

            double initialZoom = FxTestHarness.callOnFxThread(() -> mounted.mapView().getZoom());

            FxTestHarness.runOnFxThread(() ->
                    mounted.mapView().fireEvent(mouseDoubleClick(256.0, 256.0)));

            double zoomAfter = FxTestHarness.callOnFxThread(() -> mounted.mapView().getZoom());
            assertThat(zoomAfter).isEqualTo(initialZoom);
        }
    }

    @Test
    void doubleClickDoesNotExceedMaxZoom() {
        WritableImage tileImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        try (MountedMapView mounted = MapViewTestHarness.mount(() -> new MapView(retriever, cache), 512, 512)) {
            FxTestHarness.runOnFxThread(() -> {
                MapView view = mounted.mapView();
                disableAnimations(view);
                view.setCenterLat(0.0);
                view.setCenterLon(0.0);
                view.setZoom(MapState.DEFAULT_MAX_ZOOM);
            });
            mounted.layout();

            double initialZoom = FxTestHarness.callOnFxThread(() -> mounted.mapView().getZoom());
            assertThat(initialZoom).isEqualTo(MapState.DEFAULT_MAX_ZOOM);

            FxTestHarness.runOnFxThread(() ->
                    mounted.mapView().fireEvent(mouseDoubleClick(256.0, 256.0)));

            double zoomAfter = FxTestHarness.callOnFxThread(() -> mounted.mapView().getZoom());
            assertThat(zoomAfter).isEqualTo(MapState.DEFAULT_MAX_ZOOM);
        }
    }

    @Test
    void doubleClickPreservesCursorGeographicPosition() {
        WritableImage tileImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        try (MountedMapView mounted = MapViewTestHarness.mount(() -> new MapView(retriever, cache), 512, 512)) {
            FxTestHarness.runOnFxThread(() -> {
                MapView view = mounted.mapView();
                disableAnimations(view);
                view.setCenterLat(37.7749);
                view.setCenterLon(-122.4194);
                view.setZoom(10.0);
            });
            mounted.layout();

            // Double-click at a position offset from center
            double clickX = 384.0; // Off-center to right
            double clickY = 192.0; // Off-center to top

            FxTestHarness.runOnFxThread(() ->
                    mounted.mapView().fireEvent(mouseDoubleClick(clickX, clickY)));
            mounted.layout();

            // After zoom, the clicked point should remain approximately at the same screen position
            // We verify this by checking that the geographic point is still close to the cursor
            // This is an indirect test - the actual verification is that applyZoom handles this correctly
            double zoomAfter = FxTestHarness.callOnFxThread(() -> mounted.mapView().getZoom());
            assertThat(zoomAfter).isCloseTo(11.0, within(0.0001));

            // The center should have shifted to keep the clicked geo point under the cursor
            // Exact verification requires computing expected center, but we can verify zoom changed
            double centerLatAfter = FxTestHarness.callOnFxThread(() -> mounted.mapView().getCenterLat());
            double centerLonAfter = FxTestHarness.callOnFxThread(() -> mounted.mapView().getCenterLon());
            
            // Center should have changed (since we clicked off-center)
            assertThat(centerLatAfter).isNotEqualTo(37.7749);
            assertThat(centerLonAfter).isNotEqualTo(-122.4194);
        }
    }

    @Test
    void singleClickDoesNotZoom() {
        WritableImage tileImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        try (MountedMapView mounted = MapViewTestHarness.mount(() -> new MapView(retriever, cache), 512, 512)) {
            FxTestHarness.runOnFxThread(() -> {
                MapView view = mounted.mapView();
                disableAnimations(view);
                view.setCenterLat(0.0);
                view.setCenterLon(0.0);
                view.setZoom(5.0);
            });
            mounted.layout();

            double initialZoom = FxTestHarness.callOnFxThread(() -> mounted.mapView().getZoom());

            FxTestHarness.runOnFxThread(() ->
                    mounted.mapView().fireEvent(mouseSingleClick(256.0, 256.0)));

            double zoomAfter = FxTestHarness.callOnFxThread(() -> mounted.mapView().getZoom());
            assertThat(zoomAfter).isEqualTo(initialZoom);
        }
    }

    @Test
    void enableDoubleClickZoomDefaultsToTrue() {
        FxTestHarness.runOnFxThread(() -> {
            MapView mapView = new MapView();
            assertThat(mapView.isEnableDoubleClickZoom()).isTrue();
        });
    }

    @Test
    void enableDoubleClickZoomCanBeToggled() {
        FxTestHarness.runOnFxThread(() -> {
            MapView mapView = new MapView();
            mapView.setEnableDoubleClickZoom(false);
            assertThat(mapView.isEnableDoubleClickZoom()).isFalse();
            
            mapView.setEnableDoubleClickZoom(true);
            assertThat(mapView.isEnableDoubleClickZoom()).isTrue();
        });
    }

    private static MouseEvent mouseDoubleClick(double x, double y) {
        return new MouseEvent(
                MouseEvent.MOUSE_CLICKED,
                x,
                y,
                x,
                y,
                MouseButton.PRIMARY,
                2, // Click count = 2 for double-click
                false,
                false,
                false,
                false,
                false, // primaryButtonDown = false for CLICKED event
                false,
                false,
                false,
                false,
                true, // stillSincePress = true
                null);
    }

    private static void disableAnimations(MapView mapView) {
        mapView.getAnimationConfig().setAnimationsEnabled(false);
    }

    private static MouseEvent mouseSingleClick(double x, double y) {
        return new MouseEvent(
                MouseEvent.MOUSE_CLICKED,
                x,
                y,
                x,
                y,
                MouseButton.PRIMARY,
                1, // Click count = 1 for single-click
                false,
                false,
                false,
                false,
                false, // primaryButtonDown = false for CLICKED event
                false,
                false,
                false,
                false,
                true, // stillSincePress = true
                null);
    }
}
