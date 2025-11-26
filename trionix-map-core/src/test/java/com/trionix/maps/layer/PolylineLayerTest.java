package com.trionix.maps.layer;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.GeoPoint;
import com.trionix.maps.MapView;
import com.trionix.maps.testing.FxTestHarness;
import com.trionix.maps.testing.MapViewTestHarness;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Polyline;
import org.junit.jupiter.api.Test;

class PolylineLayerTest {

    @Test
    void addPolyline_rendersLine() {
        try (var mounted = MapViewTestHarness.mount(MapView::new, 512.0, 512.0)) {
            MapView view = mounted.mapView();
            FxTestHarness.runOnFxThread(() -> {
                PolylineLayer layer = new PolylineLayer();
                view.getLayers().add(layer);

                com.trionix.maps.layer.Polyline polyline = new com.trionix.maps.layer.Polyline();
                polyline.addPoint(GeoPoint.of(0.0, 0.0));
                polyline.addPoint(GeoPoint.of(10.0, 10.0));
                layer.addPolyline(polyline);
            });

            mounted.layout();

            FxTestHarness.runOnFxThread(() -> {
                PolylineLayer layer = (PolylineLayer) view.getLayers().get(0);
                // Find the JavaFX Polyline node
                Polyline lineNode = (Polyline) layer.getChildren().stream()
                        .filter(n -> n instanceof Polyline)
                        .findFirst()
                        .orElseThrow();
                
                assertThat(lineNode.getPoints()).hasSize(4); // x1, y1, x2, y2
            });
        }
    }

    @Test
    void markersVisible_rendersMarkers() {
        try (var mounted = MapViewTestHarness.mount(MapView::new, 512.0, 512.0)) {
            MapView view = mounted.mapView();
            FxTestHarness.runOnFxThread(() -> {
                PolylineLayer layer = new PolylineLayer();
                view.getLayers().add(layer);

                com.trionix.maps.layer.Polyline polyline = new com.trionix.maps.layer.Polyline();
                polyline.addPoint(GeoPoint.of(0.0, 0.0));
                polyline.setMarkersVisible(true);
                layer.addPolyline(polyline);
            });

            mounted.layout();

            FxTestHarness.runOnFxThread(() -> {
                PolylineLayer layer = (PolylineLayer) view.getLayers().get(0);
                // Should have Polyline + 1 Marker Node
                long markerCount = layer.getChildren().stream()
                        .filter(n -> !(n instanceof Polyline))
                        .count();
                assertThat(markerCount).isEqualTo(1);
            });
        }
    }

    @Test
    void dragVertex_updatesGeoPoint() {
        try (var mounted = MapViewTestHarness.mount(MapView::new, 512.0, 512.0)) {
            MapView view = mounted.mapView();
            final com.trionix.maps.layer.Polyline[] polylineRef = new com.trionix.maps.layer.Polyline[1];

            FxTestHarness.runOnFxThread(() -> {
                PolylineLayer layer = new PolylineLayer();
                view.getLayers().add(layer);

                com.trionix.maps.layer.Polyline polyline = new com.trionix.maps.layer.Polyline();
                polyline.addPoint(GeoPoint.of(0.0, 0.0));
                polyline.setEditable(true);
                polyline.setMarkersVisible(true); // Ensure markers are there to drag
                layer.addPolyline(polyline);
                polylineRef[0] = polyline;
            });

            mounted.layout();

            FxTestHarness.runOnFxThread(() -> {
                PolylineLayer layer = (PolylineLayer) view.getLayers().get(0);
                Node markerNode = layer.getChildren().stream()
                        .filter(n -> !(n instanceof Polyline))
                        .findFirst()
                        .orElseThrow();

                // Simulate drag
                double startX = markerNode.getLayoutX() + markerNode.getBoundsInLocal().getWidth() / 2.0;
                double startY = markerNode.getLayoutY() + markerNode.getBoundsInLocal().getHeight() / 2.0;
                
                // Mouse pressed
                markerNode.fireEvent(new MouseEvent(
                        MouseEvent.MOUSE_PRESSED,
                        startX, startY,
                        startX, startY,
                        MouseButton.PRIMARY, 1,
                        false, false, false, false,
                        true, false, false, false, false, false, null));

                // Mouse dragged (move 10 pixels right)
                double endX = startX + 10.0;
                double endY = startY;
                
                markerNode.fireEvent(new MouseEvent(
                        MouseEvent.MOUSE_DRAGGED,
                        endX, endY,
                        endX, endY,
                        MouseButton.PRIMARY, 1,
                        false, false, false, false,
                        true, false, false, false, false, false, null));
                
                markerNode.fireEvent(new MouseEvent(
                        MouseEvent.MOUSE_RELEASED,
                        endX, endY,
                        endX, endY,
                        MouseButton.PRIMARY, 1,
                        false, false, false, false,
                        true, false, false, false, false, false, null));
                
                // Check if point moved
                GeoPoint newPoint = polylineRef[0].getPoints().get(0);
                assertThat(newPoint.longitude()).isNotEqualTo(0.0);
            });
        }
    }
}
