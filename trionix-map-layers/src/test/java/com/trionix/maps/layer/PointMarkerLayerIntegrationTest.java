package com.trionix.maps.layer;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.MapView;
import com.trionix.maps.testing.MapViewTestHarness;
import com.trionix.maps.testing.FxTestHarness;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

class PointMarkerLayerIntegrationTest {

    @Test
    void dragging_marker_updatesLocationAndDoesNotPanMap() {
        try (var mounted = MapViewTestHarness.mount(MapView::new, 512.0, 512.0)) {
            MapView view = mounted.mapView();

            final PointMarker[] markerRef = new PointMarker[1];

            FxTestHarness.runOnFxThread(() -> {
                PointMarkerLayer layer = new PointMarkerLayer();
                view.getLayers().add(layer);
                Region node = new Region();
                node.setPrefSize(16.0, 16.0);
                PointMarker marker = layer.addMarker(view.getCenterLat(), view.getCenterLon(), node);
                marker.setDraggable(true);
                markerRef[0] = marker;
            });

            mounted.layout();

            FxTestHarness.runOnFxThread(() -> {
                PointMarker marker = markerRef[0];
                double initialLat = marker.getLatitude();
                double initialLon = marker.getLongitude();
                double mapLat = view.getCenterLat();
                double mapLon = view.getCenterLon();

                // Simulate drag using the node-local coordinates to avoid scene/local conversion
                Region node = (Region) marker.getNode();
                double startX = node.getLayoutX() + node.getBoundsInLocal().getWidth() / 2.0;
                double startY = node.getLayoutY() + node.getBoundsInLocal().getHeight() / 2.0;
                double endX = startX + 100.0;

                // Fire events directly with local coordinates (previous polyline tests used the same
                // technique and proved reliable in headless CI environments)
                node.fireEvent(mousePressed(startX, startY));
                node.fireEvent(mouseDragged(endX, startY));
                node.fireEvent(mouseReleased(endX, startY));

                // After drag, map center should remain unchanged and marker coords updated
                assertThat(view.getCenterLat()).isEqualTo(mapLat);
                assertThat(view.getCenterLon()).isEqualTo(mapLon);

                // Marker coords should have changed due to drag
                assertThat(marker.getLatitude()).isNotEqualTo(initialLat);
                assertThat(marker.getLongitude()).isNotEqualTo(initialLon);
            });
        }
    }

    @Test
    void nonDraggable_marker_doesNotMove_whenMouseDragged() {
        try (var mounted = MapViewTestHarness.mount(MapView::new, 512.0, 512.0)) {
            MapView view = mounted.mapView();

            final PointMarker[] markerRef = new PointMarker[1];

            FxTestHarness.runOnFxThread(() -> {
                PointMarkerLayer layer = new PointMarkerLayer();
                view.getLayers().add(layer);
                Region node = new Region();
                node.setPrefSize(16.0, 16.0);
                PointMarker marker = layer.addMarker(view.getCenterLat(), view.getCenterLon(), node);
                // marker remains non-draggable
                markerRef[0] = marker;
            });

            mounted.layout();

            FxTestHarness.runOnFxThread(() -> {
                PointMarker marker = markerRef[0];
                double initialLat = marker.getLatitude();
                double initialLon = marker.getLongitude();

                Region node = (Region) marker.getNode();
                double startX = node.getLayoutX() + node.getBoundsInLocal().getWidth() / 2.0;
                double startY = node.getLayoutY() + node.getBoundsInLocal().getHeight() / 2.0;
                double endX = startX + 100.0;

                node.fireEvent(mousePressed(startX, startY));
                node.fireEvent(mouseDragged(endX, startY));
                node.fireEvent(mouseReleased(endX, startY));

                // Marker coords should NOT have changed for non-draggable marker
                assertThat(marker.getLatitude()).isEqualTo(initialLat);
                assertThat(marker.getLongitude()).isEqualTo(initialLon);
            });
        }
    }
    
    private static MouseEvent mousePressed(double x, double y) {
        return new MouseEvent(
                MouseEvent.MOUSE_PRESSED,
                x,
                y,
                x,
                y,
                MouseButton.PRIMARY,
                1,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                true,
                null);
    }

    private static MouseEvent mouseDragged(double x, double y) {
        return new MouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                x,
                y,
                x,
                y,
                MouseButton.PRIMARY,
                1,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                null);
    }

    private static MouseEvent mouseReleased(double x, double y) {
        return new MouseEvent(
                MouseEvent.MOUSE_RELEASED,
                x,
                y,
                x,
                y,
                MouseButton.PRIMARY,
                1,
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
