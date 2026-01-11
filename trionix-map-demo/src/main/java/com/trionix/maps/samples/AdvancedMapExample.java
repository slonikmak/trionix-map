package com.trionix.maps.samples;

import com.trionix.maps.GeoPoint;
import com.trionix.maps.MapView;
import com.trionix.maps.SimpleOsmTileRetriever;
import com.trionix.maps.TileCache;
import com.trionix.maps.TileCacheBuilder;
import com.trionix.maps.control.ScaleRulerControl;
import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.projection.WebMercatorProjection;
import com.trionix.maps.layer.*;
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

import java.nio.file.Path;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Переработанный пример с функционалом:
 * - Маркеры: добавление, перемещение, удаление, редактирование (цвет/текст),
 * список.
 * - Линии: рисование, редактирование, удаление, список, цвет.
 */
public final class AdvancedMapExample extends Application {

    private MapView mapView;
    private PointMarkerLayer markerLayer;
    private PolylineLayer polylineLayer;
    private GridLayer gridLayer;
    private ScaleRulerControl scaleRuler;

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

    private final Projection projection = WebMercatorProjection.INSTANCE;

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
        // Use a tiered cache (L1 memory -> L2 disk) for better performance and
        // persistence.
        Path cacheDir = Path.of(System.getProperty("user.home"), ".trionix", "tiles");
        TileCache cache = TileCacheBuilder.create()
                .memory(500)
                .disk(cacheDir, 10_000)
                .build();

        mapView = new MapView(new SimpleOsmTileRetriever(), cache);
        mapView.setPrefSize(1200.0, 800.0);
        mapView.setCenterLat(55.7558);
        mapView.setCenterLon(37.6173);
        mapView.setZoom(10.0);

        // Note: input zooms are instantaneous in the current runtime (no in-flight zoom
        // animations)

        markerLayer = new PointMarkerLayer();
        polylineLayer = new PolylineLayer();
        gridLayer = new GridLayer();
        gridLayer.setVisible(false); // Скрыт по умолчанию
        mapView.getLayers().addAll(polylineLayer, markerLayer, gridLayer);

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

        // Scale Ruler
        scaleRuler = new ScaleRulerControl(mapView);
        scaleRuler.setMaxWidth(Region.USE_PREF_SIZE);
        scaleRuler.setMaxHeight(Region.USE_PREF_SIZE);
        scaleRuler.setVisible(false); // Скрыт по умолчанию
        StackPane.setAlignment(scaleRuler, Pos.BOTTOM_LEFT);
        StackPane.setMargin(scaleRuler, new Insets(10));
        mapContainer.getChildren().add(scaleRuler);

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

    // --- UI Labels with counters ---
    private Label markersHeaderLabel;
    private Label linesHeaderLabel;

    // --- Placeholder labels ---
    private Label markerPlaceholder;
    private Label linePlaceholder;

    private Node createControlPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(12));
        panel.setPrefWidth(320);
        panel.setStyle("-fx-background-color: #f8f9fa;");

        // ==================== MODES SECTION ====================
        VBox modesCard = createSectionCard();
        Label modesLabel = createSectionHeader("🎯 Режимы");

        ToggleGroup modeGroup = new ToggleGroup();

        addMarkerModeBtn = new ToggleButton("📍 Добавить маркер");
        addMarkerModeBtn.setToggleGroup(modeGroup);
        addMarkerModeBtn.setMaxWidth(Double.MAX_VALUE);
        addMarkerModeBtn.setTooltip(new Tooltip("Кликните на карту, чтобы добавить маркер"));
        styleModeButton(addMarkerModeBtn);

        drawLineModeBtn = new ToggleButton("✏️ Рисовать линию");
        drawLineModeBtn.setToggleGroup(modeGroup);
        drawLineModeBtn.setMaxWidth(Double.MAX_VALUE);
        drawLineModeBtn.setTooltip(new Tooltip("Кликайте на карту, чтобы добавлять точки линии"));
        styleModeButton(drawLineModeBtn);

        // Active mode styling
        modeGroup.selectedToggleProperty().addListener((obs, old, newToggle) -> {
            // Reset old button style
            if (old instanceof ToggleButton oldBtn) {
                styleModeButton(oldBtn);
            }

            if (newToggle == addMarkerModeBtn) {
                currentMode = InteractionMode.ADD_MARKER;
                finishDrawing();
                styleActiveModeButton(addMarkerModeBtn);
            } else if (newToggle == drawLineModeBtn) {
                currentMode = InteractionMode.DRAW_LINE;
                startDrawing();
                styleActiveModeButton(drawLineModeBtn);
            } else {
                currentMode = InteractionMode.NONE;
                finishDrawing();
            }
        });

        HBox modeButtons = new HBox(8, addMarkerModeBtn, drawLineModeBtn);
        HBox.setHgrow(addMarkerModeBtn, Priority.ALWAYS);
        HBox.setHgrow(drawLineModeBtn, Priority.ALWAYS);

        modesCard.getChildren().addAll(modesLabel, modeButtons);

        // ==================== MARKERS SECTION ====================
        VBox markersCard = createSectionCard();
        markersHeaderLabel = createSectionHeader("📍 Маркеры (0)");

        markerListView = new ListView<>(markers);
        markerListView.setPrefHeight(100);
        markerListView.setMaxWidth(Double.MAX_VALUE);
        markerListView.setPlaceholder(new Label("Нет маркеров"));
        markerListView.setTooltip(new Tooltip("Выберите маркер для редактирования"));
        markerListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PointMarker item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-text-fill: #333;");
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    MarkerData data = markerDataMap.get(item);
                    setText(data != null ? data.name : "Marker");
                    Circle indicator = new Circle(6, data != null ? data.color : Color.RED);
                    indicator.setStroke(Color.WHITE);
                    indicator.setStrokeWidth(1.5);
                    setGraphic(indicator);
                }
            }
        });
        markerListView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> selectMarker(sel));

        // Update counter when markers change
        markers.addListener((javafx.collections.ListChangeListener<PointMarker>) c -> {
            markersHeaderLabel.setText("📍 Маркеры (" + markers.size() + ")");
        });

        // Marker Placeholder (shown when no marker selected)
        markerPlaceholder = new Label("Выберите маркер для редактирования");
        markerPlaceholder.setStyle("-fx-text-fill: #888; -fx-font-style: italic; -fx-padding: 8 0;");
        markerPlaceholder.setMaxWidth(Double.MAX_VALUE);
        markerPlaceholder.setAlignment(Pos.CENTER);

        // Marker Edit Box
        markerEditBox = new VBox(8);
        markerEditBox.setStyle(
                "-fx-padding: 8; -fx-background-color: #fff; -fx-background-radius: 6; -fx-border-color: #e0e0e0; -fx-border-radius: 6;");

        Label markerPropsLabel = new Label("Свойства маркера");
        markerPropsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333; -fx-font-size: 11;");

        markerNameField = new TextField();
        markerNameField.setPromptText("Название маркера");
        markerNameField.setOnAction(e -> updateSelectedMarker());
        markerNameField.setTooltip(new Tooltip("Введите название и нажмите Enter"));

        markerColorPicker = new ColorPicker();
        markerColorPicker.setMaxWidth(Double.MAX_VALUE);
        markerColorPicker.setOnAction(e -> updateSelectedMarker());
        markerColorPicker.setTooltip(new Tooltip("Выберите цвет маркера"));

        // Compact row: name + color
        HBox markerPropsRow = new HBox(8);
        HBox.setHgrow(markerNameField, Priority.ALWAYS);
        markerColorPicker.setPrefWidth(60);
        markerPropsRow.getChildren().addAll(markerNameField, markerColorPicker);

        deleteMarkerBtn = new Button("🗑️ Удалить");
        deleteMarkerBtn.setMaxWidth(Double.MAX_VALUE);
        deleteMarkerBtn.setOnAction(e -> deleteSelectedMarker());
        deleteMarkerBtn.setTooltip(new Tooltip("Удалить выбранный маркер"));
        deleteMarkerBtn.setStyle(
                "-fx-text-fill: #c0392b; -fx-background-color: #fdecea; -fx-border-color: #e74c3c; -fx-border-radius: 4; -fx-background-radius: 4;");

        markerEditBox.getChildren().addAll(markerPropsLabel, markerPropsRow, deleteMarkerBtn);
        markerEditBox.setVisible(false);
        markerEditBox.setManaged(false);

        markersCard.getChildren().addAll(markersHeaderLabel, markerListView, markerPlaceholder, markerEditBox);

        // ==================== LINES SECTION ====================
        VBox linesCard = createSectionCard();
        linesHeaderLabel = createSectionHeader("✏️ Линии (0)");

        polylineListView = new ListView<>(polylineLayer.getPolylines());
        polylineListView.setPrefHeight(100);
        polylineListView.setMaxWidth(Double.MAX_VALUE);
        polylineListView.setPlaceholder(new Label("Нет линий"));
        polylineListView.setTooltip(new Tooltip("Выберите линию для редактирования"));
        polylineListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Polyline item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-text-fill: #333;");
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("Линия " + (getIndex() + 1) + " (" + item.getPoints().size() + " точек)");
                    javafx.scene.shape.Rectangle r = new javafx.scene.shape.Rectangle(16, 10, item.getStrokeColor());
                    r.setStroke(Color.gray(0.3));
                    r.setArcWidth(3);
                    r.setArcHeight(3);
                    setGraphic(r);
                }
            }
        });
        polylineListView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> selectPolyline(sel));

        // Update counter when polylines change
        polylineLayer.getPolylines().addListener((javafx.collections.ListChangeListener<Polyline>) c -> {
            linesHeaderLabel.setText("✏️ Линии (" + polylineLayer.getPolylines().size() + ")");
        });

        // Line Placeholder
        linePlaceholder = new Label("Выберите линию для редактирования");
        linePlaceholder.setStyle("-fx-text-fill: #888; -fx-font-style: italic; -fx-padding: 8 0;");
        linePlaceholder.setMaxWidth(Double.MAX_VALUE);
        linePlaceholder.setAlignment(Pos.CENTER);

        // Line Edit Box
        lineEditBox = new VBox(8);
        lineEditBox.setStyle(
                "-fx-padding: 8; -fx-background-color: #fff; -fx-background-radius: 6; -fx-border-color: #e0e0e0; -fx-border-radius: 6;");

        Label linePropsLabel = new Label("Свойства линии");
        linePropsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333; -fx-font-size: 11;");

        lineColorPicker = new ColorPicker();
        lineColorPicker.setMaxWidth(Double.MAX_VALUE);
        lineColorPicker.setOnAction(e -> updateSelectedLine());
        lineColorPicker.setTooltip(new Tooltip("Выберите цвет линии"));

        lineEditableCheck = new CheckBox("Редактируемая");
        lineEditableCheck.setStyle("-fx-text-fill: #333;");
        lineEditableCheck.setTooltip(new Tooltip("Позволяет перетаскивать точки линии"));
        lineEditableCheck.setOnAction(e -> {
            if (selectedPolyline != null)
                selectedPolyline.setEditable(lineEditableCheck.isSelected());
        });

        // Compact row: color + editable
        HBox linePropsRow = new HBox(8);
        HBox.setHgrow(lineColorPicker, Priority.ALWAYS);
        linePropsRow.setAlignment(Pos.CENTER_LEFT);
        linePropsRow.getChildren().addAll(lineColorPicker, lineEditableCheck);

        deleteLineBtn = new Button("🗑️ Удалить");
        deleteLineBtn.setMaxWidth(Double.MAX_VALUE);
        deleteLineBtn.setOnAction(e -> deleteSelectedLine());
        deleteLineBtn.setTooltip(new Tooltip("Удалить выбранную линию"));
        deleteLineBtn.setStyle(
                "-fx-text-fill: #c0392b; -fx-background-color: #fdecea; -fx-border-color: #e74c3c; -fx-border-radius: 4; -fx-background-radius: 4;");

        lineEditBox.getChildren().addAll(linePropsLabel, linePropsRow, deleteLineBtn);
        lineEditBox.setVisible(false);
        lineEditBox.setManaged(false);

        linesCard.getChildren().addAll(linesHeaderLabel, polylineListView, linePlaceholder, lineEditBox);

        // ==================== OVERLAYS SECTION ====================
        VBox overlaysCard = createSectionCard();
        Label overlaysLabel = createSectionHeader("🗺️ Наложения");

        // Grid row: checkbox + color picker
        CheckBox gridVisibleCheck = new CheckBox("Сетка координат");
        gridVisibleCheck.setSelected(gridLayer.isVisible());
        gridVisibleCheck.setStyle("-fx-text-fill: #333;");
        gridVisibleCheck.setTooltip(new Tooltip("Показать/скрыть сетку координат на карте"));
        gridVisibleCheck.setOnAction(e -> gridLayer.setVisible(gridVisibleCheck.isSelected()));

        ColorPicker gridColorPicker = new ColorPicker(gridLayer.getStrokeColor());
        gridColorPicker.setPrefWidth(60);
        gridColorPicker.setTooltip(new Tooltip("Цвет сетки координат"));
        gridColorPicker.setOnAction(e -> gridLayer.setStrokeColor(gridColorPicker.getValue()));

        HBox gridRow = new HBox(8);
        gridRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(gridVisibleCheck, Priority.ALWAYS);
        gridRow.getChildren().addAll(gridVisibleCheck, gridColorPicker);

        // Ruler row
        CheckBox rulerVisibleCheck = new CheckBox("Линейка масштаба");
        rulerVisibleCheck.setSelected(scaleRuler.isVisible());
        rulerVisibleCheck.setStyle("-fx-text-fill: #333;");
        rulerVisibleCheck.setTooltip(new Tooltip("Показать/скрыть линейку масштаба"));
        rulerVisibleCheck.setOnAction(e -> scaleRuler.setVisible(rulerVisibleCheck.isSelected()));

        overlaysCard.getChildren().addAll(overlaysLabel, gridRow, rulerVisibleCheck);

        // ==================== ASSEMBLE PANEL ====================
        panel.getChildren().addAll(modesCard, markersCard, linesCard, overlaysCard);

        // Scrollable container
        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPrefWidth(panel.getPrefWidth() + 20);
        scroll.setStyle("-fx-background: #f8f9fa; -fx-background-color: #f8f9fa;");

        return scroll;
    }

    private VBox createSectionCard() {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);");
        return card;
    }

    private Label createSectionHeader(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #2c3e50;");
        return label;
    }

    private void styleModeButton(ToggleButton btn) {
        btn.setStyle(
                "-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #bdc3c7; -fx-cursor: hand;");
    }

    private void styleActiveModeButton(ToggleButton btn) {
        btn.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #2980b9; -fx-font-weight: bold;");
    }

    private void handleMapClick(MouseEvent ev) {
        if (ev.getButton() != MouseButton.PRIMARY || !ev.isStillSincePress())
            return;

        // Check if we clicked on a marker (if so, don't add another one)
        // But here we are on the MapView level. If the marker consumed the event, we
        // wouldn't be here?
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
            // Note: we don't consume the event here explicitly, but usually the layer
            // handles it.
        });

        // Select the new marker
        markerListView.getSelectionModel().select(marker);
    }

    private Node createMarkerNode(String name, Color color) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        // Label at index 0
        Label l = new Label(name);
        l.setStyle(
                "-fx-background-color: rgba(255,255,255,0.9); -fx-padding: 2px 4px; -fx-font-size: 11px; -fx-background-radius: 4; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 2, 0, 0, 1);");

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

        // Toggle visibility: show edit box when selected, placeholder when not
        markerEditBox.setVisible(hasSelection);
        markerEditBox.setManaged(hasSelection);
        markerPlaceholder.setVisible(!hasSelection);
        markerPlaceholder.setManaged(!hasSelection);

        if (hasSelection) {
            MarkerData data = markerDataMap.get(marker);
            if (data != null) {
                markerNameField.setText(data.name);
                markerColorPicker.setValue(data.color);
            }

            // Animate to marker location
            mapView.flyTo(marker.getLatitude(), marker.getLongitude(), mapView.getZoom(), Duration.seconds(0.5));
        }
    }

    private void updateSelectedMarker() {
        if (selectedMarker == null)
            return;
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
        // polylineLayer.getPolylines() is observable, so list view updates
        // automatically
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

        // Toggle visibility: show edit box when selected, placeholder when not
        lineEditBox.setVisible(hasSelection);
        lineEditBox.setManaged(hasSelection);
        linePlaceholder.setVisible(!hasSelection);
        linePlaceholder.setManaged(!hasSelection);

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
