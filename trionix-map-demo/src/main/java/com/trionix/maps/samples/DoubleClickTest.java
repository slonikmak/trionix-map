package com.trionix.maps.samples;

import com.trionix.maps.MapView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Simple test for double-click zoom functionality.
 */
public class DoubleClickTest extends Application {

    @Override
    public void start(Stage stage) {
        MapView mapView = new MapView();
        mapView.setCenterLat(55.7558);
        mapView.setCenterLon(37.6173);
        mapView.setZoom(10.0);

        Label eventLog = new Label("Waiting for events...");
        eventLog.setStyle("-fx-padding: 10; -fx-background-color: rgba(255,255,255,0.9);");
        
        Label zoomLabel = new Label();
        zoomLabel.setStyle("-fx-padding: 10; -fx-background-color: rgba(255,255,255,0.9);");
        
        mapView.zoomProperty().addListener((obs, oldVal, newVal) -> {
            zoomLabel.setText(String.format("Zoom: %.2f", newVal.doubleValue()));
        });
        zoomLabel.setText(String.format("Zoom: %.2f", mapView.getZoom()));

        // Log all mouse events for debugging
        mapView.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            eventLog.setText(String.format("CLICKED: count=%d, button=%s, stillSincePress=%b", 
                e.getClickCount(), e.getButton(), e.isStillSincePress()));
            System.out.println("MOUSE_CLICKED: count=" + e.getClickCount() + 
                ", button=" + e.getButton() + 
                ", stillSincePress=" + e.isStillSincePress() +
                ", consumed=" + e.isConsumed());
        });

        mapView.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            System.out.println("MOUSE_PRESSED: button=" + e.getButton() + ", clickCount=" + e.getClickCount());
        });

        mapView.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            System.out.println("MOUSE_RELEASED: button=" + e.getButton() + ", clickCount=" + e.getClickCount());
        });

        VBox info = new VBox(5, eventLog, zoomLabel);
        StackPane root = new StackPane(mapView, info);
        StackPane.setAlignment(info, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(info, new Insets(10));

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Double-Click Zoom Test - Try double-clicking on the map!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
