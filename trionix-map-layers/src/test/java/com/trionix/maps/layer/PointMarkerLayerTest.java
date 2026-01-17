package com.trionix.maps.layer;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.MapView;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
class PointMarkerLayerTest {

    private Stage stage;
    private MapView mapView;

    @Start
    private void start(Stage stage) {
        this.stage = stage;
        stage.setScene(new Scene(new StackPane(), 512, 512));
        stage.show();
    }

    @AfterEach
    void cleanup() {
        Platform.runLater(() -> {
            if (stage != null && stage.getScene() != null) {
                ((StackPane) stage.getScene().getRoot()).getChildren().clear();
            }
            mapView = null;
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void addMarker_positionsNodeCenteredBottom() {
        mount(MapView::new, 512.0, 512.0);
        
        Platform.runLater(() -> {
            PointMarkerLayer layer = new PointMarkerLayer();
            mapView.getLayers().add(layer);

            Region node = new Region();
            node.setPrefSize(20.0, 20.0);

            layer.addMarker(0.0, 0.0, node);
            mapView.requestLayout();
            mapView.layout();
            
            // Verify
            PointMarker marker = layer.getMarkers().get(0);
            Region markerNode = (Region) marker.getNode();

            double expectedX = mapView.getWidth() / 2.0 - markerNode.getPrefWidth() / 2.0;
            double expectedY = mapView.getHeight() / 2.0 - markerNode.getPrefHeight();

            assertThat(markerNode.getLayoutX()).isEqualTo(expectedX);
            assertThat(markerNode.getLayoutY()).isEqualTo(expectedY);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void setLocation_movesNode() {
        mount(MapView::new, 512.0, 512.0);
        
        final AtomicReference<PointMarker> markerRef = new AtomicReference<>();

        Platform.runLater(() -> {
            PointMarkerLayer layer = new PointMarkerLayer();
            mapView.getLayers().add(layer);
            Region node = new Region();
            node.setPrefSize(10.0, 10.0);
            markerRef.set(layer.addMarker(0.0, 0.0, node));
            
            mapView.requestLayout();
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> {
            PointMarker marker = markerRef.get();
            marker.setLocation(10.0, 10.0);
            mapView.requestLayout();
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> {
            PointMarker marker = markerRef.get();
            Region node = (Region) marker.getNode();
            // ensure the node is not in the same place as the original center
            assertThat(node.getLayoutX()).isNotEqualTo(mapView.getWidth() / 2.0 - node.getPrefWidth() / 2.0);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void removeMarker_returnsCorrectlyAndRemovesNode() {
        mount(MapView::new, 512.0, 512.0);
        
        final AtomicReference<PointMarker> markerRef = new AtomicReference<>();

        Platform.runLater(() -> {
            PointMarkerLayer layer = new PointMarkerLayer();
            mapView.getLayers().add(layer);
            Region node = new Region();
            node.setPrefSize(8.0, 8.0);
            markerRef.set(layer.addMarker(0.0, 0.0, node));
            
            mapView.requestLayout();
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> {
            PointMarkerLayer layer = (PointMarkerLayer) mapView.getLayers().get(0);
            boolean removed = layer.removeMarker(markerRef.get());
            assertThat(removed).isTrue();
            assertThat(layer.getMarkers()).isEmpty();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void clickHandler_isInvoked() {
        mount(MapView::new, 512.0, 512.0);
        
        final AtomicReference<PointMarker> markerRef = new AtomicReference<>();
        final boolean[] invoked = new boolean[1];

        Platform.runLater(() -> {
            PointMarkerLayer layer = new PointMarkerLayer();
            mapView.getLayers().add(layer);
            Region node = new Region();
            node.setPrefSize(12.0, 12.0);
            PointMarker marker = layer.addMarker(0.0, 0.0, node);
            marker.setOnClick(m -> invoked[0] = true);
            markerRef.set(marker);
            
            mapView.requestLayout();
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> {
            PointMarker marker = markerRef.get();
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
        });
        WaitForAsyncUtils.waitForFxEvents();
        
        assertThat(invoked[0]).isTrue();
    }

    @Test
    void invisible_marker_doesNotReceiveClicks() {
        mount(MapView::new, 512.0, 512.0);
        
        final AtomicReference<PointMarker> markerRef = new AtomicReference<>();
        final boolean[] invoked = new boolean[1];

        Platform.runLater(() -> {
            PointMarkerLayer layer = new PointMarkerLayer();
            mapView.getLayers().add(layer);
            Region node = new Region();
            node.setPrefSize(12.0, 12.0);
            PointMarker marker = layer.addMarker(0.0, 0.0, node);
            marker.setOnClick(m -> invoked[0] = true);
            marker.setVisible(false);
            markerRef.set(marker);
            
            mapView.requestLayout();
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> {
            PointMarker marker = markerRef.get();
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
        });
        WaitForAsyncUtils.waitForFxEvents();
        
        assertThat(invoked[0]).isFalse();
    }
    
    private void mount(java.util.function.Supplier<MapView> factory, double width, double height) {
        Platform.runLater(() -> {
            this.mapView = factory.get();
            StackPane root = (StackPane) stage.getScene().getRoot();
            root.getChildren().setAll(mapView);
            mapView.resize(width, height);
            mapView.requestLayout();
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }
}
