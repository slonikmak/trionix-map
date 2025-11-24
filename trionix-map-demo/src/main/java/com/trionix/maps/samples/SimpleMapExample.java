package com.trionix.maps.samples;

import com.trionix.maps.MapView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Самый простой пример использования MapView - карта без дополнительных элементов.
 * Демонстрирует минимальный код, необходимый для отображения карты.
 */
public final class SimpleMapExample extends Application {

    @Override
    public void start(Stage stage) {
        // Создаем карту
        MapView mapView = new MapView();
        
        // Устанавливаем начальную позицию (Москва)
        mapView.setCenterLat(55.7558);
        mapView.setCenterLon(37.6173);
        mapView.setZoom(10.0);
        
        // Устанавливаем размер
        mapView.setPrefSize(800.0, 600.0);
        
        // Создаем сцену и отображаем окно
        StackPane root = new StackPane(mapView);
        Scene scene = new Scene(root, 800.0, 600.0);
        
        stage.setTitle("Простой пример карты");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
