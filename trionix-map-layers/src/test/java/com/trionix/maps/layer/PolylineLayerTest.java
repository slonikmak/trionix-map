package com.trionix.maps.layer;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.GeoPoint;
import com.trionix.maps.MapView;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polyline;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
class PolylineLayerTest {

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
    void addPolyline_rendersLine() {
        mount(MapView::new, 512.0, 512.0);

        Platform.runLater(() -> {
            PolylineLayer layer = new PolylineLayer();
            mapView.getLayers().add(layer);

            com.trionix.maps.layer.Polyline polyline = new com.trionix.maps.layer.Polyline();
            polyline.addPoint(GeoPoint.of(0.0, 0.0));
            polyline.addPoint(GeoPoint.of(10.0, 10.0));
            layer.addPolyline(polyline);
            
            mapView.requestLayout();
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> {
            PolylineLayer layer = (PolylineLayer) mapView.getLayers().get(0);
            Polyline lineNode = (Polyline) layer.getChildren().stream()
                    .filter(n -> n instanceof Polyline)
                    .findFirst()
                    .orElseThrow();
            
            assertThat(lineNode.getPoints()).hasSize(4); // x1, y1, x2, y2
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void markersVisible_rendersMarkers() {
        mount(MapView::new, 512.0, 512.0);

        Platform.runLater(() -> {
            PolylineLayer layer = new PolylineLayer();
            mapView.getLayers().add(layer);

            com.trionix.maps.layer.Polyline polyline = new com.trionix.maps.layer.Polyline();
            polyline.addPoint(GeoPoint.of(0.0, 0.0));
            polyline.setMarkersVisible(true);
            layer.addPolyline(polyline);
            
            mapView.requestLayout();
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> {
            PolylineLayer layer = (PolylineLayer) mapView.getLayers().get(0);
            long markerCount = layer.getChildren().stream()
                    .filter(n -> !(n instanceof Polyline))
                    .count();
            assertThat(markerCount).isEqualTo(1);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void dragVertex_updatesGeoPoint() {
        mount(MapView::new, 512.0, 512.0);
        
        final AtomicReference<com.trionix.maps.layer.Polyline> polylineRef = new AtomicReference<>();

        Platform.runLater(() -> {
            PolylineLayer layer = new PolylineLayer();
            mapView.getLayers().add(layer);

            com.trionix.maps.layer.Polyline polyline = new com.trionix.maps.layer.Polyline();
            polyline.addPoint(GeoPoint.of(0.0, 0.0));
            polyline.setEditable(true);
            polyline.setMarkersVisible(true);
            layer.addPolyline(polyline);
            polylineRef.set(polyline);
            
            mapView.requestLayout();
            mapView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> {
            PolylineLayer layer = (PolylineLayer) mapView.getLayers().get(0);
            Node markerNode = layer.getChildren().stream()
                    .filter(n -> !(n instanceof Polyline))
                    .findFirst()
                    .orElseThrow();

            double startX = markerNode.getLayoutX() + markerNode.getBoundsInLocal().getWidth() / 2.0;
            double startY = markerNode.getLayoutY() + markerNode.getBoundsInLocal().getHeight() / 2.0;
            
            markerNode.fireEvent(new MouseEvent(
                    MouseEvent.MOUSE_PRESSED,
                    startX, startY,
                    startX, startY,
                    MouseButton.PRIMARY, 1,
                    false, false, false, false,
                    true, false, false, false, false, false, null));

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
            
            GeoPoint newPoint = polylineRef.get().getPoints().get(0);
            assertThat(newPoint.longitude()).isNotEqualTo(0.0);
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
}
