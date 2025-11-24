package com.trionix.maps.testing;

import com.trionix.maps.MapView;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;

/**
 * Utility for mounting a {@link MapView} inside a headless JavaFX scene so integration
 * tests can drive layout, snapshots, and event dispatch without a visible stage.
 */
public final class MapViewTestHarness {

    private MapViewTestHarness() {
    }

    /**
     * Creates a fully initialized {@link MapView} inside an off-screen JavaFX scene.
     * The supplied factory runs on the JavaFX thread. Callers should close the returned
     * handle to release scene graph references between tests.
     */
    public static MountedMapView mount(Supplier<MapView> factory, double width, double height) {
        return mount(factory, width, height, view -> {
        });
    }

    public static MountedMapView mount(
            Supplier<MapView> factory, double width, double height, Consumer<MapView> initializer) {
        return FxTestHarness.callOnFxThread(() -> {
            MapView mapView = Objects.requireNonNull(factory.get(), "mapView");
            if (initializer != null) {
                initializer.accept(mapView);
            }
            StackPane root = new StackPane(mapView);
            Scene scene = new Scene(root, width, height);
            root.applyCss();
            root.layout();
            mapView.resize(width, height);
            mapView.requestLayout();
            mapView.layout();
            return new MountedMapView(mapView, scene, root);
        });
    }

    /** Handle for interacting with the mounted {@link MapView}. */
    public static final class MountedMapView implements AutoCloseable {
        private final MapView mapView;
        private final Scene scene;
        private final StackPane root;

        private MountedMapView(MapView mapView, Scene scene, StackPane root) {
            this.mapView = mapView;
            this.scene = scene;
            this.root = root;
        }

        public MapView mapView() {
            return mapView;
        }

        /** Applies CSS and forces another layout pass on the JavaFX thread. */
        public void layout() {
            FxTestHarness.runOnFxThread(() -> {
                root.applyCss();
                root.layout();
                mapView.requestLayout();
                mapView.layout();
            });
        }

        /** Resizes the map view and immediately re-lays out the scene. */
        public void resize(double width, double height) {
            FxTestHarness.runOnFxThread(() -> {
                mapView.resize(width, height);
                mapView.requestLayout();
                mapView.layout();
            });
        }

        /** Blocks until pending JavaFX runLater tasks have been processed. */
        public void flushFx() {
            FxTestHarness.runOnFxThread(() -> {
            });
        }

        /** Captures an off-screen snapshot of the map contents. */
        public WritableImage snapshot() {
            return FxTestHarness.callOnFxThread(() -> mapView.snapshot(null, null));
        }

        @Override
        public void close() {
            FxTestHarness.runOnFxThread(() -> {
                root.getChildren().clear();
                scene.setRoot(new StackPane());
            });
        }
    }
}
