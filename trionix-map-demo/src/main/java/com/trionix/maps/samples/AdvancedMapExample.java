package com.trionix.maps.samples;

import com.trionix.maps.GeoPoint;
import com.trionix.maps.MapView;
import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;
import com.trionix.maps.layer.PointMarker;
import com.trionix.maps.layer.PointMarkerLayer;
import com.trionix.maps.layer.Polyline;
import com.trionix.maps.layer.PolylineLayer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Переработанный пример с функционалом:
 * - Маркеры: добавление, перемещение, удаление, редактирование (цвет/текст), список.
 * - Линии: рисование, редактирование, удаление, список, цвет.
 */
public final class AdvancedMapExample extends Application {

    private MapView mapView;
    private PointMarkerLayer markerLayer;
    private PolylineLayer polylineLayer;

    // Data
    private final ObservableList<PointMarker> markers = FXCollections.observableArrayList();
    private final Map<PointMarker, MarkerData> markerDataMap = new IdentityHashMap<>();
    // Polyline list is managed by the layer, we use it directly

    // State
    private PointMarker selectedMarker;
    private Polyline selectedPolyline;
    private Polyline currentDrawingPolyline;

    private enum InteractionMode {
        NONE,
        ADD_MARKER,
        DRAW_LINE
    }
    private InteractionMode currentMode = InteractionMode.NONE;

    // UI Controls
    private Label infoLabel;
    private ListView<PointMarker> markerListView;
    private ListView<Polyline> polylineListView;

    // Marker Edit Controls
    private VBox markerEditBox;
    private TextField markerNameField;
    private ColorPicker markerColorPicker;
    private Button deleteMarkerBtn;

    // Line Edit Controls
    private VBox lineEditBox;
    private ColorPicker lineColorPicker;
    private CheckBox lineEditableCheck;
    private Button deleteLineBtn;

    // Toggles
    private ToggleButton addMarkerModeBtn;
    private ToggleButton drawLineModeBtn;

    private final Projection projection = new WebMercatorProjection();

    private static class MarkerData {
        String name;
        Color color;
        MarkerData(String name, Color color) {
            this.name = name;
            this.color = color;
        }
    }

    @Override
    public void start(Stage stage) {
        mapView = new MapView();
        mapView.setPrefSize(1200.0, 800.0);
        mapView.setCenterLat(55.7558);
        mapView.setCenterLon(37.6173);
        mapView.setZoom(10.0);

        markerLayer = new PointMarkerLayer();
        polylineLayer = new PolylineLayer();
        mapView.getLayers().addAll(polylineLayer, markerLayer);

        // Setup UI
        BorderPane root = new BorderPane();
        StackPane mapContainer = new StackPane(mapView);
        root.setCenter(mapContainer);

        // Info Label
        infoLabel = new Label();
        infoLabel.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-padding: 10; -fx-font-size: 14;");
        StackPane.setAlignment(infoLabel, Pos.TOP_LEFT);
        StackPane.setMargin(infoLabel, new Insets(10));
        mapContainer.getChildren().add(infoLabel);
        updateInfoLabel();

        // Control Panel
        root.setRight(createControlPanel());

        // Event Handlers
        mapView.centerLatProperty().addListener(o -> updateInfoLabel());
        mapView.centerLonProperty().addListener(o -> updateInfoLabel());
        mapView.zoomProperty().addListener(o -> updateInfoLabel());

        mapView.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMapClick);

        Scene scene = new Scene(root, 1400, 800);
        stage.setTitle("Редактор Карты");
        stage.setScene(scene);
        stage.show();
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));
        panel.setPrefWidth(350);
        panel.setStyle("-fx-background-color: #f5f5f5;");

        // --- Modes ---
        Label modesLabel = new Label("Режимы");
        modesLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        ToggleGroup modeGroup = new ToggleGroup();
        addMarkerModeBtn = new ToggleButton("Добавить маркер");
        addMarkerModeBtn.setToggleGroup(modeGroup);
        addMarkerModeBtn.setMaxWidth(Double.MAX_VALUE);

        drawLineModeBtn = new ToggleButton("Рисовать линию");
        drawLineModeBtn.setToggleGroup(modeGroup);
        drawLineModeBtn.setMaxWidth(Double.MAX_VALUE);

        modeGroup.selectedToggleProperty().addListener((obs, old, newToggle) -> {
            if (newToggle == addMarkerModeBtn) {
                currentMode = InteractionMode.ADD_MARKER;
                finishDrawing();
            } else if (newToggle == drawLineModeBtn) {
                currentMode = InteractionMode.DRAW_LINE;
                startDrawing();
            } else {
                currentMode = InteractionMode.NONE;
                finishDrawing();
            }
        });

        // --- Markers ---
        Label markersLabel = new Label("Маркеры");
        markersLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        markerListView = new ListView<>(markers);
        markerListView.setPrefHeight(150);
        markerListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PointMarker item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    MarkerData data = markerDataMap.get(item);
                    setText(data != null ? data.name : "Marker");
                }
            }
        });
        markerListView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> selectMarker(sel));

        // Marker Edit
        markerEditBox = new VBox(8);
        markerNameField = new TextField();
        markerNameField.setPromptText("Название");
        markerNameField.setOnAction(e -> updateSelectedMarker());

        markerColorPicker = new ColorPicker();
        markerColorPicker.setMaxWidth(Double.MAX_VALUE);
        markerColorPicker.setOnAction(e -> updateSelectedMarker());

        deleteMarkerBtn = new Button("Удалить маркер");
        deleteMarkerBtn.setMaxWidth(Double.MAX_VALUE);
        deleteMarkerBtn.setOnAction(e -> deleteSelectedMarker());

        markerEditBox.getChildren().addAll(new Label("Свойства маркера:"), markerNameField, markerColorPicker, deleteMarkerBtn);
        markerEditBox.setDisable(true);

        // --- Lines ---
        Label linesLabel = new Label("Линии");
        linesLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        polylineListView = new ListView<>(polylineLayer.getPolylines());
        polylineListView.setPrefHeight(150);
        polylineListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Polyline item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("Линия " + (getIndex() + 1) + " (" + item.getPoints().size() + " точек)");
                    javafx.scene.shape.Rectangle r = new javafx.scene.shape.Rectangle(16, 10, item.getStrokeColor());
                    r.setStroke(Color.BLACK);
                    setGraphic(r);
                }
            }
        });
        polylineListView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> selectPolyline(sel));

        // Line Edit
        lineEditBox = new VBox(8);
        lineColorPicker = new ColorPicker();
        lineColorPicker.setMaxWidth(Double.MAX_VALUE);
        lineColorPicker.setOnAction(e -> updateSelectedLine());

        lineEditableCheck = new CheckBox("Редактируемая");
        lineEditableCheck.setOnAction(e -> {
            if (selectedPolyline != null) selectedPolyline.setEditable(lineEditableCheck.isSelected());
        });

        deleteLineBtn = new Button("Удалить линию");
        deleteLineBtn.setMaxWidth(Double.MAX_VALUE);
        deleteLineBtn.setOnAction(e -> deleteSelectedLine());

        lineEditBox.getChildren().addAll(new Label("Свойства линии:"), lineColorPicker, lineEditableCheck, deleteLineBtn);
        lineEditBox.setDisable(true);

        panel.getChildren().addAll(
                modesLabel, addMarkerModeBtn, drawLineModeBtn,
                new Separator(),
                markersLabel, markerListView, markerEditBox,
                new Separator(),
                linesLabel, polylineListView, lineEditBox
        );
        return panel;
    }

    private void handleMapClick(MouseEvent ev) {
        if (ev.getButton() != MouseButton.PRIMARY || !ev.isStillSincePress()) return;

        // Check if we clicked on a marker (if so, don't add another one)
        // But here we are on the MapView level. If the marker consumed the event, we wouldn't be here?
        // Actually, let's rely on the mode.

        GeoPoint loc = getClickLocation(ev);

        if (currentMode == InteractionMode.ADD_MARKER) {
            addMarker(loc.latitude(), loc.longitude(), "Маркер " + (markers.size() + 1), Color.RED);
        } else if (currentMode == InteractionMode.DRAW_LINE) {
            if (currentDrawingPolyline != null) {
                currentDrawingPolyline.addPoint(loc);
                // Refresh list to show point count update
                polylineListView.refresh();
            }
        }
    }

    private GeoPoint getClickLocation(MouseEvent ev) {
        var local = mapView.sceneToLocal(ev.getSceneX(), ev.getSceneY());
        int zoom = (int) Math.floor(mapView.getZoom());
        var centerPx = projection.latLonToPixel(mapView.getCenterLat(), mapView.getCenterLon(), zoom);
        double dx = local.getX() - mapView.getWidth() / 2.0;
        double dy = local.getY() - mapView.getHeight() / 2.0;
        var latLon = projection.pixelToLatLon(centerPx.x() + dx, centerPx.y() + dy, zoom);
        return GeoPoint.of(latLon.latitude(), latLon.longitude());
    }

    private void addMarker(double lat, double lon, String name, Color color) {
        Node node = createMarkerNode(name, color);
        PointMarker marker = markerLayer.addMarker(lat, lon, node);
        marker.setDraggable(true);
        markerDataMap.put(marker, new MarkerData(name, color));
        markers.add(marker);

        marker.setOnClick(m -> {
            markerListView.getSelectionModel().select(m);
            // Note: we don't consume the event here explicitly, but usually the layer handles it.
        });
        
        // Select the new marker
        markerListView.getSelectionModel().select(marker);
    }

    private Node createMarkerNode(String name, Color color) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        // Label at index 0
        Label l = new Label(name);
        l.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-padding: 2px 4px; -fx-font-size: 11px; -fx-background-radius: 4; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 2, 0, 0, 1);");
        
        // Circle at index 1
        Circle c = new Circle(8, color);
        c.setStroke(Color.WHITE);
        c.setStrokeWidth(2);
        c.setEffect(new javafx.scene.effect.DropShadow(3, Color.gray(0.5)));
        
        box.getChildren().addAll(l, c);
        return box;
    }

    private void selectMarker(PointMarker marker) {
        selectedMarker = marker;
        boolean hasSelection = (marker != null);

        markerEditBox.setDisable(!hasSelection);

        if (hasSelection) {
            MarkerData data = markerDataMap.get(marker);
            if (data != null) {
                markerNameField.setText(data.name);
                markerColorPicker.setValue(data.color);
            }

            // Center map
            mapView.flyTo(marker.getLatitude(), marker.getLongitude(), mapView.getZoom(), Duration.seconds(0.5));
            
            // Highlight visual (optional, could add a border to the node)
        }
    }

    private void updateSelectedMarker() {
        if (selectedMarker == null) return;
        String name = markerNameField.getText();
        Color color = markerColorPicker.getValue();

        MarkerData data = markerDataMap.get(selectedMarker);
        if (data != null) {
            data.name = name;
            data.color = color;
        }

        // Update visual
        VBox node = (VBox) selectedMarker.getNode();
        if (node.getChildren().size() >= 2) {
            Label l = (Label) node.getChildren().get(0);
            l.setText(name);
            Circle c = (Circle) node.getChildren().get(1);
            c.setFill(color);
        }

        markerListView.refresh();
    }

    private void deleteSelectedMarker() {
        if (selectedMarker != null) {
            markerLayer.removeMarker(selectedMarker);
            markers.remove(selectedMarker);
            markerDataMap.remove(selectedMarker);
            markerListView.getSelectionModel().clearSelection();
        }
    }

    // ... Line methods ...
    private void startDrawing() {
        currentDrawingPolyline = new Polyline();
        currentDrawingPolyline.setStrokeColor(Color.BLUE);
        currentDrawingPolyline.setStrokeWidth(4);
        currentDrawingPolyline.setEditable(true);
        polylineLayer.addPolyline(currentDrawingPolyline);
        // polylineLayer.getPolylines() is observable, so list view updates automatically
        polylineListView.getSelectionModel().select(currentDrawingPolyline);
    }

    private void finishDrawing() {
        if (currentDrawingPolyline != null) {
            // If line has < 2 points, maybe remove it?
            if (currentDrawingPolyline.getPoints().size() < 2) {
                polylineLayer.removePolyline(currentDrawingPolyline);
            }
            currentDrawingPolyline = null;
        }
    }

    private void selectPolyline(Polyline polyline) {
        selectedPolyline = polyline;
        boolean hasSelection = (polyline != null);
        lineEditBox.setDisable(!hasSelection);

        if (hasSelection) {
            lineColorPicker.setValue(polyline.getStrokeColor());
            lineEditableCheck.setSelected(polyline.isEditable());
        }
    }

    private void updateSelectedLine() {
        if (selectedPolyline != null) {
            selectedPolyline.setStrokeColor(lineColorPicker.getValue());
            polylineLayer.requestLayerLayout();
            polylineListView.refresh();
        }
    }

    private void deleteSelectedLine() {
        if (selectedPolyline != null) {
            polylineLayer.removePolyline(selectedPolyline);
            polylineListView.getSelectionModel().clearSelection();
            if (selectedPolyline == currentDrawingPolyline) {
                currentDrawingPolyline = null;
            }
        }
    }

    private void updateInfoLabel() {
        infoLabel.setText(String.format("Широта: %.4f  Долгота: %.4f  Масштаб: %.2f",
                mapView.getCenterLat(), mapView.getCenterLon(), mapView.getZoom()));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
