package com.trionix.maps.internal.tiles;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javafx.scene.image.Image;

/** Provides access to the shared placeholder tile image. */
public final class PlaceholderTileFactory {

    private static final Image PLACEHOLDER = loadPlaceholder();

    private PlaceholderTileFactory() {
    }

    public static Image placeholder() {
        return PLACEHOLDER;
    }

    private static Image loadPlaceholder() {
        try (InputStream stream = PlaceholderTileFactory.class
                .getResourceAsStream("/com/trionix/maps/placeholder-tile.png")) {
            return new Image(Objects.requireNonNull(stream, "Missing placeholder tile resource"));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load placeholder tile", e);
        }
    }
}
