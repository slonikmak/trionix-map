package com.trionix.maps.internal.interaction;

import com.trionix.maps.MapView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;

/**
 * Handles user interaction events (drag, scroll, click, pinch) for MapView.
 * <p>
 * This class encapsulates all input event processing logic to keep MapView
 * focused on rendering and state management.
 */
public final class MapInteractionHandler {

    private static final double SCROLL_ZOOM_STEP = 0.5;
    private static final double DOUBLE_CLICK_ZOOM_DELTA = 1.0;

    private final MapView mapView;
    private boolean dragging;
    private double lastDragX;
    private double lastDragY;

    /**
     * Creates a new interaction handler for the specified MapView.
     *
     * @param mapView the map view to handle interactions for
     */
    public MapInteractionHandler(MapView mapView) {
        this.mapView = mapView;
    }

    /**
     * Installs all event handlers on the MapView.
     */
    public void install() {
        mapView.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        mapView.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        mapView.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        mapView.addEventHandler(MouseEvent.MOUSE_EXITED, this::handleMouseReleased);
        mapView.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked);
        mapView.addEventHandler(ScrollEvent.SCROLL, this::handleScroll);
        mapView.addEventHandler(ZoomEvent.ZOOM, this::handleZoomGesture);
        mapView.addEventHandler(ZoomEvent.ZOOM_STARTED, this::handleZoomGestureStarted);
        mapView.addEventHandler(ZoomEvent.ZOOM_FINISHED, this::handleZoomGestureFinished);
    }

    private void handleMousePressed(MouseEvent event) {
        if (!event.isPrimaryButtonDown()) {
            return;
        }
        mapView.cancelActiveAnimation();
        dragging = true;
        lastDragX = event.getX();
        lastDragY = event.getY();
    }

    private void handleMouseDragged(MouseEvent event) {
        if (!dragging || !event.isPrimaryButtonDown()) {
            return;
        }
        double deltaX = event.getX() - lastDragX;
        double deltaY = event.getY() - lastDragY;
        lastDragX = event.getX();
        lastDragY = event.getY();
        mapView.panByPixelsDelta(deltaX, deltaY);
    }

    private void handleMouseReleased(MouseEvent event) {
        dragging = false;
    }

    private void handleMouseClicked(MouseEvent event) {
        if (!mapView.isEnableDoubleClickZoom()
                || event.getButton() != MouseButton.PRIMARY
                || event.getClickCount() != 2) {
            return;
        }
        if (event.isConsumed()) {
            return;
        }
        if (!event.isStillSincePress()) {
            return;
        }
        mapView.cancelActiveAnimation();
        mapView.zoomAroundPointBy(DOUBLE_CLICK_ZOOM_DELTA, event.getX(), event.getY());
    }

    private void handleScroll(ScrollEvent event) {
        if (event.isInertia()) {
            return;
        }
        double delta = event.getDeltaY();
        if (delta == 0.0) {
            return;
        }
        mapView.cancelActiveAnimation();
        double zoomDelta = SCROLL_ZOOM_STEP * Math.signum(delta);
        mapView.zoomAroundPointBy(zoomDelta, event.getX(), event.getY());
        event.consume();
    }

    private void handleZoomGesture(ZoomEvent event) {
        double factor = event.getZoomFactor();
        if (factor <= 0.0 || factor == 1.0) {
            return;
        }
        mapView.cancelActiveAnimation();
        double zoomDelta = Math.log(factor) / Math.log(2.0);
        mapView.zoomAroundPointBy(zoomDelta, event.getX(), event.getY());
        event.consume();
    }

    private void handleZoomGestureStarted(ZoomEvent event) {
        mapView.cancelActiveAnimation();
    }

    private void handleZoomGestureFinished(ZoomEvent event) {
        // No momentum or delayed animation when pinch ends
    }
}
