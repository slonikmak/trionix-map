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
class PointMarkerLayerIntegrationTest {

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
    void dragging_marker_updatesLocationAndDoesNotPanMap() {
        mount(MapView::new, 512.0, 512.0);

        final AtomicReference<PointMarker> markerRef = new AtomicReference<>();

        Platform.runLater(() -> {
            PointMarkerLayer layer = new PointMarkerLayer();
            mapView.getLayers().add(layer);
            Region node = new Region();
            node.setPrefSize(16.0, 16.0);
            PointMarker marker = layer.addMarker(mapView.getCenterLat(), mapView.getCenterLon(), node);
            marker.setDraggable(true);
            markerRef.set(marker);
            
            mapView.requestLayout();
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> {
            PointMarker marker = markerRef.get();
            double initialLat = marker.getLatitude();
            double initialLon = marker.getLongitude();
            double mapLat = mapView.getCenterLat();
            double mapLon = mapView.getCenterLon();

            Region node = (Region) marker.getNode();
            double startX = node.getLayoutX() + node.getBoundsInLocal().getWidth() / 2.0;
            double startY = node.getLayoutY() + node.getBoundsInLocal().getHeight() / 2.0;
            double endX = startX + 100.0;

            node.fireEvent(mousePressed(startX, startY));
            node.fireEvent(mouseDragged(endX, startY));
            node.fireEvent(mouseReleased(endX, startY));

            assertThat(mapView.getCenterLat()).isEqualTo(mapLat);
            assertThat(mapView.getCenterLon()).isEqualTo(mapLon);

            assertThat(marker.getLatitude()).isNotEqualTo(initialLat);
            assertThat(marker.getLongitude()).isNotEqualTo(initialLon);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void nonDraggable_marker_doesNotMove_whenMouseDragged() {
        mount(MapView::new, 512.0, 512.0);

        final AtomicReference<PointMarker> markerRef = new AtomicReference<>();

        Platform.runLater(() -> {
            PointMarkerLayer layer = new PointMarkerLayer();
            mapView.getLayers().add(layer);
            Region node = new Region();
            node.setPrefSize(16.0, 16.0);
            PointMarker marker = layer.addMarker(mapView.getCenterLat(), mapView.getCenterLon(), node);
            markerRef.set(marker);
            
            mapView.requestLayout();
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> {
            PointMarker marker = markerRef.get();
            double initialLat = marker.getLatitude();
            double initialLon = marker.getLongitude();

            Region node = (Region) marker.getNode();
            double startX = node.getLayoutX() + node.getBoundsInLocal().getWidth() / 2.0;
            double startY = node.getLayoutY() + node.getBoundsInLocal().getHeight() / 2.0;
            double endX = startX + 100.0;

            node.fireEvent(mousePressed(startX, startY));
            node.fireEvent(mouseDragged(endX, startY));
            node.fireEvent(mouseReleased(endX, startY));

            assertThat(marker.getLatitude()).isEqualTo(initialLat);
            assertThat(marker.getLongitude()).isEqualTo(initialLon);
        });
        WaitForAsyncUtils.waitForFxEvents();
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
