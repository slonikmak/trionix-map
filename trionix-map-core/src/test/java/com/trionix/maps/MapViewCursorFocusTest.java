package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;
import com.trionix.maps.testing.FxTestHarness;
import com.trionix.maps.testing.MapViewTestHarness;
import com.trionix.maps.testing.MapViewTestHarness.MountedMapView;
import java.util.concurrent.CompletableFuture;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;

/**
 * Tests cursor-focus preservation during user-initiated zoom interactions.
 * The spec requires that the geographic point under the cursor remains within ±2 pixels
 * of its original screen position after scroll-wheel and double-click zooms (zoom is immediate).
 */
class MapViewCursorFocusTest {

    private static final double PIXEL_TOLERANCE = 2.0;
    private final Projection projection = new WebMercatorProjection();

    @Test
    void scrollZoomPreservesCursorFocusWithinTwoPixels() throws InterruptedException {
        WritableImage tileImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        try (MountedMapView mounted = MapViewTestHarness.mount(() -> new MapView(retriever, cache), 512, 512)) {
            FxTestHarness.runOnFxThread(() -> {
                MapView view = mounted.mapView();
                // Input zooms are immediate in the runtime behavior — animations are not applied
                view.getAnimationConfig().setAnimationsEnabled(false);
                view.setCenterLat(37.7749);
                view.setCenterLon(-122.4194);
                view.setZoom(10.0);
            });
            mounted.layout();

            // Scroll at an off-center position
            double scrollX = 384.0;
            double scrollY = 192.0;

            // Capture the geographic point under the cursor before zoom
            Projection.LatLon geoPointBefore = FxTestHarness.callOnFxThread(() ->
                    latLonAt(mounted.mapView(), scrollX, scrollY));

                // Trigger scroll zoom and verify focus immediately
                FxTestHarness.runOnFxThread(() ->
                    mounted.mapView().fireEvent(createScrollEvent(120.0, scrollX, scrollY)));
            double pixelShift = FxTestHarness.callOnFxThread(() ->
                    computePixelShift(mounted.mapView(), geoPointBefore, scrollX, scrollY));

            assertThat(pixelShift)
                    .describedAs("Geographic point should remain within ±2px of cursor during scroll zoom")
                    .isLessThanOrEqualTo(PIXEL_TOLERANCE);
        }
    }

    @Test
    void doubleClickZoomPreservesCursorFocusWithinTwoPixels() throws InterruptedException {
        WritableImage tileImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        try (MountedMapView mounted = MapViewTestHarness.mount(() -> new MapView(retriever, cache), 512, 512)) {
            FxTestHarness.runOnFxThread(() -> {
                MapView view = mounted.mapView();
                view.getAnimationConfig().setAnimationsEnabled(true);
                view.getAnimationConfig().setDoubleClickZoomAnimationEnabled(true);
                view.getAnimationConfig().setDoubleClickZoomDuration(Duration.millis(250));
                view.setCenterLat(37.7749);
                view.setCenterLon(-122.4194);
                view.setZoom(10.0);
            });
            mounted.layout();

            // Double-click at an off-center position
            double clickX = 320.0;
            double clickY = 256.0;

            // Capture the geographic point under the cursor before zoom
            Projection.LatLon geoPointBefore = FxTestHarness.callOnFxThread(() ->
                    latLonAt(mounted.mapView(), clickX, clickY));

                // Trigger double-click zoom and verify focus immediately
                FxTestHarness.runOnFxThread(() ->
                    mounted.mapView().fireEvent(mouseDoubleClick(clickX, clickY)));
            double pixelShift = FxTestHarness.callOnFxThread(() ->
                    computePixelShift(mounted.mapView(), geoPointBefore, clickX, clickY));

            assertThat(pixelShift)
                    .describedAs("Geographic point should remain within ±2px of cursor during double-click zoom")
                    .isLessThanOrEqualTo(PIXEL_TOLERANCE);
        }
    }

    @Test
    void scrollZoomAtMapCenterPreservesCenter() throws InterruptedException {
        WritableImage tileImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        try (MountedMapView mounted = MapViewTestHarness.mount(() -> new MapView(retriever, cache), 512, 512)) {
            FxTestHarness.runOnFxThread(() -> {
                MapView view = mounted.mapView();
                view.getAnimationConfig().setAnimationsEnabled(false);
                view.setCenterLat(40.7128);
                view.setCenterLon(-74.0060);
                view.setZoom(8.0);
            });
            mounted.layout();

            double initialLat = FxTestHarness.callOnFxThread(() -> mounted.mapView().getCenterLat());
            double initialLon = FxTestHarness.callOnFxThread(() -> mounted.mapView().getCenterLon());

            // Scroll at the exact center of the map
                FxTestHarness.runOnFxThread(() ->
                    mounted.mapView().fireEvent(createScrollEvent(120.0, 256.0, 256.0)));

            double finalLat = FxTestHarness.callOnFxThread(() -> mounted.mapView().getCenterLat());
            double finalLon = FxTestHarness.callOnFxThread(() -> mounted.mapView().getCenterLon());
            double finalZoom = FxTestHarness.callOnFxThread(() -> mounted.mapView().getZoom());

            // When scrolling at center, the center coordinates should remain nearly unchanged
            assertThat(finalLat).isCloseTo(initialLat, within(0.01));
            assertThat(finalLon).isCloseTo(initialLon, within(0.01));
            assertThat(finalZoom).isCloseTo(8.5, within(0.05));
        }
    }

    @Test
    void multipleScrollEventsPreserveCursorFocus() throws InterruptedException {
        WritableImage tileImage = FxTestHarness.callOnFxThread(() -> new WritableImage(256, 256));
        TileRetriever retriever = (zoom, x, y) -> CompletableFuture.completedFuture(tileImage);
        InMemoryTileCache cache = new InMemoryTileCache(128);

        try (MountedMapView mounted = MapViewTestHarness.mount(() -> new MapView(retriever, cache), 512, 512)) {
            FxTestHarness.runOnFxThread(() -> {
                MapView view = mounted.mapView();
                view.getAnimationConfig().setAnimationsEnabled(false);
                view.setCenterLat(51.5074);
                view.setCenterLon(-0.1278);
                view.setZoom(9.0);
            });
            mounted.layout();

            double scrollX = 400.0;
            double scrollY = 300.0;

            // Capture initial geographic point
            Projection.LatLon geoPoint = FxTestHarness.callOnFxThread(() ->
                    latLonAt(mounted.mapView(), scrollX, scrollY));

            // Perform three consecutive scroll zoom operations
            FxTestHarness.runOnFxThread(() -> {
                mounted.mapView().fireEvent(createScrollEvent(120.0, scrollX, scrollY));
                mounted.mapView().fireEvent(createScrollEvent(120.0, scrollX, scrollY));
                mounted.mapView().fireEvent(createScrollEvent(120.0, scrollX, scrollY));
            });

            // After all zooms, the original geographic point should still be within tolerance
            double pixelShift = FxTestHarness.callOnFxThread(() ->
                    computePixelShift(mounted.mapView(), geoPoint, scrollX, scrollY));

            assertThat(pixelShift)
                    .describedAs("Geographic point should remain within ±2px after multiple scroll zooms")
                    .isLessThanOrEqualTo(PIXEL_TOLERANCE);
        }
    }

    /**
     * Computes the geographic coordinates at a given screen position.
     */
    private Projection.LatLon latLonAt(MapView mapView, double sceneX, double sceneY) {
        double width = mapView.getWidth();
        double height = mapView.getHeight();
        if (width <= 0.0 || height <= 0.0) {
            return null;
        }
        int zoomLevel = mapView.getZoom() >= 1.0 ? (int) mapView.getZoom() : 1;
        Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                mapView.getCenterLat(), mapView.getCenterLon(), zoomLevel);
        double offsetX = sceneX - width / 2.0;
        double offsetY = sceneY - height / 2.0;
        double pixelX = centerPixels.x() + offsetX;
        double pixelY = centerPixels.y() + offsetY;
        return projection.pixelToLatLon(pixelX, pixelY, zoomLevel);
    }

    /**
     * Computes how many pixels a geographic point has shifted from its expected screen position.
     */
    private double computePixelShift(MapView mapView, Projection.LatLon geoPoint, double expectedX, double expectedY) {
        if (geoPoint == null) {
            return Double.MAX_VALUE;
        }
        double width = mapView.getWidth();
        double height = mapView.getHeight();
        if (width <= 0.0 || height <= 0.0) {
            return Double.MAX_VALUE;
        }
        int zoomLevel = mapView.getZoom() >= 1.0 ? (int) mapView.getZoom() : 1;
        Projection.PixelCoordinate geoPixels = projection.latLonToPixel(
                geoPoint.latitude(), geoPoint.longitude(), zoomLevel);
        Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                mapView.getCenterLat(), mapView.getCenterLon(), zoomLevel);
        double actualX = (geoPixels.x() - centerPixels.x()) + width / 2.0;
        double actualY = (geoPixels.y() - centerPixels.y()) + height / 2.0;
        double dx = actualX - expectedX;
        double dy = actualY - expectedY;
        return Math.sqrt(dx * dx + dy * dy);
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

    private static MouseEvent mouseDoubleClick(double x, double y) {
        return new MouseEvent(
                MouseEvent.MOUSE_CLICKED,
                x,
                y,
                x,
                y,
                MouseButton.PRIMARY,
                2,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                null);
    }
}
