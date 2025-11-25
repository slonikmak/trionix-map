package com.trionix.maps.samples;

import com.trionix.maps.MapView;
import com.trionix.maps.layer.PointMarker;
import com.trionix.maps.layer.PointMarkerLayer;
import com.trionix.maps.layer.MapLayer;
import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.CheckBox;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * Продвинутый пример использования {@link MapView} с демонстрацией анимации,
 * множественных слоев и интерактивных элементов.
 */
public final class AdvancedMapExample extends Application {

    private MapView mapView;
    private Label infoLabel;

    // Marker layer and demo UI state
    private PointMarkerLayer markerLayer;
    private final ObservableList<PointMarker> demoMarkers = FXCollections.observableArrayList();
    private final Map<PointMarker, String> markerNames = new IdentityHashMap<>();
    private PointMarker selectedDemoMarker;
    private ListView<PointMarker> markerListView;
    private TextField selectedLatField;
    private TextField selectedLonField;
    // Projection helper used by the demo for click->lat/lon conversion
    private final Projection clickProjection = new WebMercatorProjection();
    // toggle control exposed to selection logic so UI shows draggable state
    private ToggleButton draggableToggleBtn;

    @Override
    public void start(Stage stage) {
        // Создаем компонент карты
        mapView = new MapView();
        mapView.setPrefSize(1200.0, 800.0);
        mapView.setCenterLat(55.7558); // Москва
        mapView.setCenterLon(37.6173);
        mapView.setZoom(10.0);

        // Create info label early so handlers can safely reference it
        infoLabel = new Label();
        infoLabel.setStyle("-fx-background-color: rgba(255,255,255,0.9); "
            + "-fx-padding: 10; -fx-font-size: 14;");
        updateInfoLabel();

        // Создаем слой с маркерами
        markerLayer = new PointMarkerLayer();
        PointMarker moscow = markerLayer.addMarker(55.7558, 37.6173, createMarker("Москва", Color.RED));
        PointMarker spb = markerLayer.addMarker(59.9343, 30.3351, createMarker("Санкт-Петербург", Color.BLUE));
        PointMarker kazan = markerLayer.addMarker(55.7887, 49.1221, createMarker("Казань", Color.GREEN));
        // keep a registry used by the demo UI
        markerNames.put(moscow, "Москва");
        demoMarkers.add(moscow);
        markerNames.put(spb, "Санкт-Петербург");
        demoMarkers.add(spb);
        markerNames.put(kazan, "Казань");
        demoMarkers.add(kazan);
        // Demonstrate interaction: make Moscow marker draggable and clickable
        moscow.setDraggable(true);
        moscow.setOnClick(p -> selectDemoMarker(p));
        moscow.setOnLocationChanged(p -> { if (p == selectedDemoMarker) updateSelectedCoordinatesUI(p); });

        // set onClick for the other markers too so clicking picks them
        spb.setOnClick(p -> selectDemoMarker(p));
        spb.setOnLocationChanged(p -> { if (p == selectedDemoMarker) updateSelectedCoordinatesUI(p); });
        kazan.setOnClick(p -> selectDemoMarker(p));
        kazan.setOnLocationChanged(p -> { if (p == selectedDemoMarker) updateSelectedCoordinatesUI(p); });

        // Создаем слой с линиями
        RouteLayer routeLayer = new RouteLayer();
        routeLayer.addRoute(55.7558, 37.6173, 59.9343, 30.3351, Color.PURPLE);
        routeLayer.addRoute(55.7558, 37.6173, 55.7887, 49.1221, Color.ORANGE);

        // Добавляем слои на карту
        mapView.getLayers().addAll(routeLayer, markerLayer);

        // Создаем панель управления
        VBox controlPanel = createControlPanel();

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
        panel.setPrefWidth(420);

        Label title = new Label("Управление картой");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");


        // Кнопки управления масштабом
        HBox zoomControls = new HBox(10);
        Button zoomInBtn = new Button("Zoom +");
        Button zoomOutBtn = new Button("Zoom -");
        zoomInBtn.setOnAction(e -> mapView.setZoom(mapView.getZoom() + 1));
        zoomOutBtn.setOnAction(e -> mapView.setZoom(mapView.getZoom() - 1));
        zoomControls.getChildren().addAll(zoomInBtn, zoomOutBtn);

        panel.getChildren().addAll(
                title,
                // quick navigation removed — use marker list to move to points
                new Label("Масштаб:"),
                zoomControls
        );

        // --- Marker management UI ------------------------------------------------
        Label markerTitle = new Label("Маркеры");
        markerTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        TextField nameField = new TextField();
        nameField.setPromptText("Имя маркера");

        TextField latField = new TextField();
        latField.setPromptText("Широта (например 55.75)");
        TextField lonField = new TextField();
        lonField.setPromptText("Долгота (например 37.62)");

        Button addAtCenterBtn = new Button("Добавить в центр");
        addAtCenterBtn.setOnAction(e -> {
            String name = nameField.getText().isBlank() ? "Marker " + (demoMarkers.size() + 1) : nameField.getText();
            PointMarker m = markerLayer.addMarker(mapView.getCenterLat(), mapView.getCenterLon(), createMarker(name, Color.DARKGRAY));
            markerNames.put(m, name);
            demoMarkers.add(m);
            m.setOnClick(p -> selectDemoMarker(p));
            m.setOnLocationChanged(p -> { if (p == selectedDemoMarker) updateSelectedCoordinatesUI(p); });
        });

        Button addAtLatLonBtn = new Button("Добавить по координатам");
        addAtLatLonBtn.setOnAction(e -> {
            try {
                double lat = Double.parseDouble(latField.getText());
                double lon = Double.parseDouble(lonField.getText());
                String name = nameField.getText().isBlank() ? String.format("%.4f,%.4f", lat, lon) : nameField.getText();
                PointMarker m = markerLayer.addMarker(lat, lon, createMarker(name, Color.DARKBLUE));
                markerNames.put(m, name);
                demoMarkers.add(m);
                m.setOnClick(p -> selectDemoMarker(p));
                m.setOnLocationChanged(p -> { if (p == selectedDemoMarker) updateSelectedCoordinatesUI(p); });
            } catch (NumberFormatException ex) {
                infoLabel.setText("Ошибка: неверные координаты");
            }
        });

        markerListView = new ListView<>(demoMarkers);
        ListView<PointMarker> listView = markerListView;
        listView.setMaxHeight(180);
        listView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(PointMarker item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(markerNames.getOrDefault(item, "marker"));
                }
            }
        });
        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            selectDemoMarker(sel);
            if (sel != null) {
                // center the map to the selected marker (keep current zoom)
                mapView.flyTo(sel.getLatitude(), sel.getLongitude(), mapView.getZoom(), Duration.seconds(0.6));
            }
        });

        Button deleteBtn = new Button("Удалить");
        deleteBtn.setOnAction(e -> {
            PointMarker sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                markerLayer.removeMarker(sel);
                demoMarkers.remove(sel);
                markerNames.remove(sel);
                if (sel == selectedDemoMarker) {
                    selectDemoMarker(null);
                }
            }
        });

        Button moveToCenterBtn = new Button("Переместить в центр");
        moveToCenterBtn.setOnAction(e -> {
            PointMarker sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                sel.setLocation(mapView.getCenterLat(), mapView.getCenterLon());
            }
        });

        draggableToggleBtn = new ToggleButton("Режим перетаскивания");
        draggableToggleBtn.setMaxWidth(Double.MAX_VALUE);
        draggableToggleBtn.setOnAction(e -> {
            PointMarker sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                boolean enabled = draggableToggleBtn.isSelected();
                sel.setDraggable(enabled);
            }
        });

        CheckBox addOnClick = new CheckBox("Добавлять маркер по клику");
        EventHandler<MouseEvent> clickHandler = ev -> {
            if (ev.getButton() != MouseButton.PRIMARY || !ev.isStillSincePress()) {
                return;
            }
            var local = mapView.sceneToLocal(ev.getSceneX(), ev.getSceneY());
            int zoomLevel = Math.max(0, (int) Math.floor(mapView.getZoom()));
            Projection.PixelCoordinate centerPixels = clickProjection.latLonToPixel(
                    mapView.getCenterLat(), mapView.getCenterLon(), zoomLevel);
            double offsetX = local.getX() - mapView.getWidth() / 2.0;
            double offsetY = local.getY() - mapView.getHeight() / 2.0;
            double pixelX = centerPixels.x() + offsetX;
            double pixelY = centerPixels.y() + offsetY;
            var latlon = clickProjection.pixelToLatLon(pixelX, pixelY, zoomLevel);
            String name = nameField.getText().isBlank() ? String.format("%.4f,%.4f", latlon.latitude(), latlon.longitude()) : nameField.getText();
            PointMarker m = markerLayer.addMarker(latlon.latitude(), latlon.longitude(), createMarker(name, Color.CORAL));
            markerNames.put(m, name);
            demoMarkers.add(m);
            m.setOnClick(p -> selectDemoMarker(p));
            m.setOnLocationChanged(p -> { if (p == selectedDemoMarker) updateSelectedCoordinatesUI(p); });
        };

        // attach/detach the click handler when checkbox toggles
        addOnClick.selectedProperty().addListener((obs, old, nw) -> {
            if (nw) {
                mapView.addEventHandler(MouseEvent.MOUSE_CLICKED, clickHandler);
            } else {
                mapView.removeEventHandler(MouseEvent.MOUSE_CLICKED, clickHandler);
            }
        });

        // create readonly fields to display the coordinates of the selected marker
        selectedLatField = new TextField();
        selectedLatField.setEditable(false);
        selectedLatField.setPromptText("Широта");
        selectedLonField = new TextField();
        selectedLonField.setEditable(false);
        selectedLonField.setPromptText("Долгота");

        panel.getChildren().addAll(
                new Label(""),
                markerTitle,
                nameField,
                new HBox(8, latField, lonField),
                new HBox(8, addAtCenterBtn, addAtLatLonBtn),
                listView,
            new HBox(8, deleteBtn, moveToCenterBtn),
            new Label("Координаты выделенного маркера:"),
            new HBox(8, selectedLatField, selectedLonField)
        );

            HBox toggleRow = new HBox(8, draggableToggleBtn);
            HBox.setHgrow(draggableToggleBtn, Priority.ALWAYS);
            panel.getChildren().add(toggleRow);

            // finally add the click toggle control at the bottom
            panel.getChildren().add(addOnClick);

        return panel;
    }

    // Navigation helper removed — movement now happens by selecting markers in the list

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

    private void selectDemoMarker(PointMarker marker) {
        // clear previous highlight
        if (selectedDemoMarker != null && selectedDemoMarker.getNode() != null) {
            selectedDemoMarker.getNode().setStyle("");
        }
        selectedDemoMarker = marker;
        // reflect selection in the list view
        if (markerListView != null) {
            if (marker == null) {
                markerListView.getSelectionModel().clearSelection();
            } else {
                markerListView.getSelectionModel().select(marker);
                markerListView.scrollTo(marker);
            }
        }
        if (marker != null && marker.getNode() != null) {
            marker.getNode().setStyle("-fx-border-color: #FFD54F; -fx-border-width: 2; -fx-padding: 2;");
            infoLabel.setText("Selected: " + markerNames.getOrDefault(marker, String.format("%.4f,%.4f", marker.getLatitude(), marker.getLongitude())));
            updateSelectedCoordinatesUI(marker);
            if (draggableToggleBtn != null) {
                draggableToggleBtn.setSelected(marker.isDraggable());
            }
        }
        else {
            if (draggableToggleBtn != null) {
                draggableToggleBtn.setSelected(false);
            }
            // clear coordinate display
            if (selectedLatField != null) selectedLatField.setText("");
            if (selectedLonField != null) selectedLonField.setText("");
        }
    }

    private void updateSelectedCoordinatesUI(PointMarker marker) {
        if (marker == null) return;
        if (selectedLatField != null) {
            selectedLatField.setText(String.format("%.6f", marker.getLatitude()));
        }
        if (selectedLonField != null) {
            selectedLonField.setText(String.format("%.6f", marker.getLongitude()));
        }
    }

    // Using library PointMarkerLayer in place of demo's MarkerLayer; interaction enabled above

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
            int zoomLevel = (int) Math.floor(mapView.getZoom());
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
