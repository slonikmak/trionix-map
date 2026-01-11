package com.trionix.maps.layer;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.MapView;
import com.trionix.maps.testing.FxTestHarness;
import com.trionix.maps.testing.MapViewTestHarness;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

class PointMarkerLayerTest {

    @Test
    void addMarker_positionsNodeCenteredBottom() {
        try (var mounted = MapViewTestHarness.mount(MapView::new, 512.0, 512.0)) {
            MapView view = mounted.mapView();
            FxTestHarness.runOnFxThread(() -> {
                PointMarkerLayer layer = new PointMarkerLayer();
                view.getLayers().add(layer);

                Region node = new Region();
                node.setPrefSize(20.0, 20.0);

                layer.addMarker(0.0, 0.0, node);
            });

            mounted.layout();
            FxTestHarness.runOnFxThread(() -> {
                PointMarkerLayer layer = (PointMarkerLayer) view.getLayers().get(0);
                PointMarker marker = layer.getMarkers().get(0);
                Region node = (Region) marker.getNode();

                double expectedX = view.getWidth() / 2.0 - node.getPrefWidth() / 2.0;
                double expectedY = view.getHeight() / 2.0 - node.getPrefHeight();

                assertThat(node.getLayoutX()).isEqualTo(expectedX);
                assertThat(node.getLayoutY()).isEqualTo(expectedY);
            });
        }
    }

    @Test
    void setLocation_movesNode() {
        try (var mounted = MapViewTestHarness.mount(MapView::new, 512.0, 512.0)) {
            MapView view = mounted.mapView();
            final PointMarker[] markerRef = new PointMarker[1];

            FxTestHarness.runOnFxThread(() -> {
                PointMarkerLayer layer = new PointMarkerLayer();
                view.getLayers().add(layer);
                Region node = new Region();
                node.setPrefSize(10.0, 10.0);
                markerRef[0] = layer.addMarker(0.0, 0.0, node);
            });

            mounted.layout();

            FxTestHarness.runOnFxThread(() -> {
                PointMarker marker = markerRef[0];
                marker.setLocation(10.0, 10.0);
            });

            mounted.layout();

            FxTestHarness.runOnFxThread(() -> {
                PointMarker marker = markerRef[0];
                Region node = (Region) marker.getNode();
                // ensure the node is not in the same place as the original center
                assertThat(node.getLayoutX()).isNotEqualTo(view.getWidth() / 2.0 - node.getPrefWidth() / 2.0);
            });
        }
    }

    @Test
    void removeMarker_returnsCorrectlyAndRemovesNode() {
        try (var mounted = MapViewTestHarness.mount(MapView::new, 512.0, 512.0)) {
            MapView view = mounted.mapView();
            final PointMarker[] markerRef = new PointMarker[1];

            FxTestHarness.runOnFxThread(() -> {
                PointMarkerLayer layer = new PointMarkerLayer();
                view.getLayers().add(layer);
                Region node = new Region();
                node.setPrefSize(8.0, 8.0);
                markerRef[0] = layer.addMarker(0.0, 0.0, node);
            });

            mounted.layout();

            FxTestHarness.runOnFxThread(() -> {
                PointMarkerLayer layer = (PointMarkerLayer) view.getLayers().get(0);
                boolean removed = layer.removeMarker(markerRef[0]);
                assertThat(removed).isTrue();
                assertThat(layer.getMarkers()).isEmpty();
            });
        }
    }

    @Test
    void clickHandler_isInvoked() {
        try (var mounted = MapViewTestHarness.mount(MapView::new, 512.0, 512.0)) {
            MapView view = mounted.mapView();
            final PointMarker[] markerRef = new PointMarker[1];
            final boolean[] invoked = new boolean[1];

            FxTestHarness.runOnFxThread(() -> {
                PointMarkerLayer layer = new PointMarkerLayer();
                view.getLayers().add(layer);
                Region node = new Region();
                node.setPrefSize(12.0, 12.0);
                PointMarker marker = layer.addMarker(0.0, 0.0, node);
                marker.setOnClick(m -> invoked[0] = true);
                markerRef[0] = marker;
            });

            mounted.layout();

            FxTestHarness.runOnFxThread(() -> {
                PointMarker marker = markerRef[0];
                Region node = (Region) marker.getNode();
                // fire click on node
                node.fireEvent(new MouseEvent(
                        MouseEvent.MOUSE_CLICKED,
                        6.0, 6.0,
                        6.0, 6.0,
                        MouseButton.PRIMARY,
                        1,
                        false, false, false, false,
                        false, false, false, false, false,
                        false, null));
                assertThat(invoked[0]).isTrue();
            });
        }
    }

    @Test
    void invisible_marker_doesNotReceiveClicks() {
        try (var mounted = MapViewTestHarness.mount(MapView::new, 512.0, 512.0)) {
            MapView view = mounted.mapView();
            final PointMarker[] markerRef = new PointMarker[1];
            final boolean[] invoked = new boolean[1];

            FxTestHarness.runOnFxThread(() -> {
                PointMarkerLayer layer = new PointMarkerLayer();
                view.getLayers().add(layer);
                Region node = new Region();
                node.setPrefSize(12.0, 12.0);
                PointMarker marker = layer.addMarker(0.0, 0.0, node);
                marker.setOnClick(m -> invoked[0] = true);
                marker.setVisible(false);
                markerRef[0] = marker;
            });

            mounted.layout();

            FxTestHarness.runOnFxThread(() -> {
                PointMarker marker = markerRef[0];
                Region node = (Region) marker.getNode();
                node.fireEvent(new MouseEvent(
                        MouseEvent.MOUSE_CLICKED,
                        6.0, 6.0,
                        6.0, 6.0,
                        MouseButton.PRIMARY,
                        1,
                        false, false, false, false,
                        false, false, false, false, false,
                        false, null));
                assertThat(invoked[0]).isFalse();
            });
        }
    }
}
