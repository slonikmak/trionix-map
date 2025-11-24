package com.trionix.maps.samples;

import com.trionix.maps.MapView;
import com.trionix.maps.layer.MapLayer;
import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Продвинутый пример использования {@link MapView} с демонстрацией анимации,
 * множественных слоев и интерактивных элементов.
 */
public final class AdvancedMapExample extends Application {

    private MapView mapView;
    private Label infoLabel;

    @Override
    public void start(Stage stage) {
        // Создаем компонент карты
        mapView = new MapView();
        mapView.setPrefSize(1200.0, 800.0);
        mapView.setCenterLat(55.7558); // Москва
        mapView.setCenterLon(37.6173);
        mapView.setZoom(10.0);

        // Создаем слой с маркерами
        MarkerLayer markerLayer = new MarkerLayer();
        markerLayer.addMarker(55.7558, 37.6173, createMarker("Москва", Color.RED));
        markerLayer.addMarker(59.9343, 30.3351, createMarker("Санкт-Петербург", Color.BLUE));
        markerLayer.addMarker(55.7887, 49.1221, createMarker("Казань", Color.GREEN));

        // Создаем слой с линиями
        RouteLayer routeLayer = new RouteLayer();
        routeLayer.addRoute(55.7558, 37.6173, 59.9343, 30.3351, Color.PURPLE);
        routeLayer.addRoute(55.7558, 37.6173, 55.7887, 49.1221, Color.ORANGE);

        // Добавляем слои на карту
        mapView.getLayers().addAll(routeLayer, markerLayer);

        // Создаем панель управления
        VBox controlPanel = createControlPanel();

        // Создаем информационную панель
        infoLabel = new Label();
        infoLabel.setStyle("-fx-background-color: rgba(255,255,255,0.9); "
                + "-fx-padding: 10; -fx-font-size: 14;");
        updateInfoLabel();

        // Подписываемся на изменения свойств карты
        mapView.centerLatProperty().addListener((obs, old, newVal) -> updateInfoLabel());
        mapView.centerLonProperty().addListener((obs, old, newVal) -> updateInfoLabel());
        mapView.zoomProperty().addListener((obs, old, newVal) -> updateInfoLabel());

        // Размещаем элементы
        StackPane mapContainer = new StackPane(mapView);
        StackPane.setAlignment(infoLabel, Pos.TOP_LEFT);
        StackPane.setMargin(infoLabel, new Insets(10));
        mapContainer.getChildren().add(infoLabel);

        BorderPane root = new BorderPane();
        root.setCenter(mapContainer);
        root.setRight(controlPanel);

        Scene scene = new Scene(root, 1400.0, 800.0);
        stage.setTitle("Продвинутый пример MapView");
        stage.setScene(scene);
        stage.show();
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #f5f5f5;");
        panel.setPrefWidth(200);

        Label title = new Label("Управление картой");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        // Кнопки для быстрой навигации
        Button moscowBtn = createNavigationButton("Москва", 55.7558, 37.6173, 12.0);
        Button spbBtn = createNavigationButton("Санкт-Петербург", 59.9343, 30.3351, 12.0);
        Button kazanBtn = createNavigationButton("Казань", 55.7887, 49.1221, 12.0);
        Button russiaBtn = createNavigationButton("Россия", 61.5240, 105.3188, 3.0);

        // Кнопки управления масштабом
        HBox zoomControls = new HBox(10);
        Button zoomInBtn = new Button("Zoom +");
        Button zoomOutBtn = new Button("Zoom -");
        zoomInBtn.setOnAction(e -> mapView.setZoom(mapView.getZoom() + 1));
        zoomOutBtn.setOnAction(e -> mapView.setZoom(mapView.getZoom() - 1));
        zoomControls.getChildren().addAll(zoomInBtn, zoomOutBtn);

        panel.getChildren().addAll(
                title,
                new Label("Быстрая навигация:"),
                moscowBtn,
                spbBtn,
                kazanBtn,
                russiaBtn,
                new Label("Масштаб:"),
                zoomControls
        );

        return panel;
    }

    private Button createNavigationButton(String name, double lat, double lon, double zoom) {
        Button btn = new Button(name);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> mapView.flyTo(lat, lon, zoom, Duration.seconds(1.5)));
        return btn;
    }

    private void updateInfoLabel() {
        String info = String.format(
                "Широта: %.4f°\nДолгота: %.4f°\nМасштаб: %.2f",
                mapView.getCenterLat(),
                mapView.getCenterLon(),
                mapView.getZoom()
        );
        infoLabel.setText(info);
    }

    private Region createMarker(String label, Color color) {
        StackPane marker = new StackPane();
        marker.setMouseTransparent(true);

        // Создаем круг для маркера
        Circle circle = new Circle(12);
        circle.setFill(color);
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2);

        // Создаем подпись
        Label labelNode = new Label(label);
        labelNode.setStyle(String.format(
                "-fx-background-color: rgba(%d,%d,%d,0.95); "
                        + "-fx-text-fill: white; "
                        + "-fx-padding: 4 8 4 8; "
                        + "-fx-background-radius: 10; "
                        + "-fx-font-size: 12; "
                        + "-fx-font-weight: bold;",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        ));

        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(labelNode, circle);

        marker.getChildren().add(content);
        return marker;
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Слой для отображения маркеров на карте.
     */
    private static final class MarkerLayer extends MapLayer {
        private final Projection projection = new WebMercatorProjection();
        private final List<Marker> markers = new ArrayList<>();

        void addMarker(double latitude, double longitude, Region node) {
            Objects.requireNonNull(node, "node");
            node.setManaged(false);
            getChildren().add(node);
            markers.add(new Marker(latitude, longitude, node));
            requestLayerLayout();
        }

        @Override
        public void layoutLayer(MapView mapView) {
            if (markers.isEmpty()) {
                return;
            }
            double width = getWidth();
            double height = getHeight();
            if (width <= 0.0 || height <= 0.0) {
                return;
            }
            int zoomLevel = (int) Math.round(mapView.getZoom());
            Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                    mapView.getCenterLat(), mapView.getCenterLon(), zoomLevel);
            double halfWidth = width / 2.0;
            double halfHeight = height / 2.0;

            for (Marker marker : markers) {
                Projection.PixelCoordinate markerPixels = projection.latLonToPixel(
                        marker.latitude(), marker.longitude(), zoomLevel);
                double screenX = markerPixels.x() - centerPixels.x() + halfWidth;
                double screenY = markerPixels.y() - centerPixels.y() + halfHeight;
                Region node = marker.node();
                double markerWidth = node.prefWidth(-1);
                double markerHeight = node.prefHeight(-1);
                node.resizeRelocate(
                        screenX - markerWidth / 2.0,
                        screenY - markerHeight,
                        markerWidth,
                        markerHeight
                );
            }
        }

        private record Marker(double latitude, double longitude, Region node) {
        }
    }

    /**
     * Слой для отображения маршрутов (линий) между точками на карте.
     */
    private static final class RouteLayer extends MapLayer {
        private final Projection projection = new WebMercatorProjection();
        private final List<Route> routes = new ArrayList<>();

        void addRoute(double lat1, double lon1, double lat2, double lon2, Color color) {
            Line line = new Line();
            line.setStroke(color);
            line.setStrokeWidth(3);
            line.setManaged(false);
            line.setMouseTransparent(true);
            getChildren().add(line);
            routes.add(new Route(lat1, lon1, lat2, lon2, line));
            requestLayerLayout();
        }

        @Override
        public void layoutLayer(MapView mapView) {
            if (routes.isEmpty()) {
                return;
            }
            double width = getWidth();
            double height = getHeight();
            if (width <= 0.0 || height <= 0.0) {
                return;
            }
            int zoomLevel = (int) Math.round(mapView.getZoom());
            Projection.PixelCoordinate centerPixels = projection.latLonToPixel(
                    mapView.getCenterLat(), mapView.getCenterLon(), zoomLevel);
            double halfWidth = width / 2.0;
            double halfHeight = height / 2.0;

            for (Route route : routes) {
                Projection.PixelCoordinate start = projection.latLonToPixel(
                        route.lat1(), route.lon1(), zoomLevel);
                Projection.PixelCoordinate end = projection.latLonToPixel(
                        route.lat2(), route.lon2(), zoomLevel);

                double startX = start.x() - centerPixels.x() + halfWidth;
                double startY = start.y() - centerPixels.y() + halfHeight;
                double endX = end.x() - centerPixels.x() + halfWidth;
                double endY = end.y() - centerPixels.y() + halfHeight;

                route.line().setStartX(startX);
                route.line().setStartY(startY);
                route.line().setEndX(endX);
                route.line().setEndY(endY);
            }
        }

        private record Route(double lat1, double lon1, double lat2, double lon2, Line line) {
        }
    }
}
