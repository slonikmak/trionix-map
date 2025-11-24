package com.trionix.maps.layer;

import com.trionix.maps.MapView;
import java.util.Objects;
import javafx.application.Platform;
import javafx.scene.layout.Pane;

/**
 * Base class for custom overlays rendered above {@link MapView} tiles. Subclasses must perform
 * all scene-graph mutations from the JavaFX Application Thread and should minimize work inside
 * {@link #layoutLayer(MapView)} to keep map rendering smooth.
 */
public abstract class MapLayer extends Pane {

    private MapView mapView;

    /**
     * Creates a layer with unmanaged layout so the {@link MapView} can control its bounds.
     */
    protected MapLayer() {
        setManaged(false);
        setPickOnBounds(false);
    }

    /**
     * Called once per JavaFX pulse to layout the layer relative to the current map state. This
     * method always executes on the JavaFX Application Thread.
     */
    public abstract void layoutLayer(MapView mapView);

    /**
     * Lifecycle hook invoked when the layer is added to a {@link MapView}. Subclasses typically use
     * this hook to initialize resources; the call occurs on the JavaFX Application Thread.
     */
    public void layerAdded(MapView mapView) {
        // no-op by default
    }

    /**
     * Lifecycle hook invoked when the layer is removed from a {@link MapView}. Default is no-op and
     * the call occurs on the JavaFX Application Thread.
     */
    public void layerRemoved(MapView mapView) {
        // no-op by default
    }

    /**
     * Requests that {@link #layoutLayer(MapView)} be called on the next JavaFX pulse. The request is
     * marshalled to the JavaFX Application Thread if necessary.
     */
    public final void requestLayerLayout() {
        MapView owner = mapView;
        if (owner == null) {
            return;
        }
        Runnable request = owner::requestLayout;
        if (Platform.isFxApplicationThread()) {
            request.run();
        } else {
            Platform.runLater(request);
        }
    }

    /**
     * Returns the {@link MapView} this layer is currently attached to, or {@code null} if the layer
     * has not been added to a map.
     */
    public final MapView getMapView() {
        return mapView;
    }

    /**
     * Internal hook used by {@link MapView} to register ownership. Not intended for user code.
     */
    public final void attachToMapView(MapView mapView) {
        Objects.requireNonNull(mapView, "mapView");
        if (this.mapView != null && this.mapView != mapView) {
            throw new IllegalStateException("Layer is already attached to a MapView");
        }
        this.mapView = mapView;
    }

    /**
     * Internal hook used by {@link MapView} to clear ownership. Not intended for user code.
     */
    public final void detachFromMapView(MapView mapView) {
        if (this.mapView == mapView) {
            this.mapView = null;
        }
    }
}
