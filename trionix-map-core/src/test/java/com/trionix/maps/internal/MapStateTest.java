package com.trionix.maps.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.trionix.maps.internal.projection.Projection;
import com.trionix.maps.internal.tiles.TileCoordinate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MapStateTest {

    @Test
    void calculatesVisibleTilesAcrossFullViewport() {
        MapState state = new MapState();
        state.setCenterLat(0.0);
        state.setCenterLon(0.0);
        state.setZoom(1.0);
        state.setViewportSize(512.0, 512.0);

        List<TileCoordinate> tiles = state.visibleTiles();

        assertThat(tiles).containsExactly(
                new TileCoordinate(1, 0, 0),
                new TileCoordinate(1, 1, 0),
                new TileCoordinate(1, 0, 1),
                new TileCoordinate(1, 1, 1));
    }

    @Test
    void wrapsLongitudeWhenViewportCrossesAntimeridian() {
        MapState state = new MapState();
        state.setCenterLat(0.0);
        state.setCenterLon(179.5);
        state.setZoom(2.0);
        state.setViewportSize(512.0, 256.0);

        List<TileCoordinate> tiles = state.visibleTiles();

        assertThat(tiles.stream().map(TileCoordinate::x)).contains(0L, 3L);
        assertThat(tiles).allMatch(tile -> tile.zoom() == 2);
    }

    @Test
    void clampsLatitudeToValidTileRange() {
        MapState state = new MapState();
        state.setCenterLat(Projection.MAX_LATITUDE);
        state.setCenterLon(0.0);
        state.setZoom(3.0);
        state.setViewportSize(512.0, 512.0);

        List<TileCoordinate> tiles = state.visibleTiles();
        long maxTileIndex = (1L << state.discreteZoomLevel()) - 1;

        assertThat(tiles).allMatch(tile -> tile.y() >= 0 && tile.y() <= maxTileIndex);
    }

    @Test
    void returnsEmptyListWhenViewportIsZeroSized() {
        MapState state = new MapState();
        state.setCenterLat(0.0);
        state.setCenterLon(0.0);
        state.setZoom(5.0);
        state.setViewportSize(0.0, 0.0);

        assertThat(state.visibleTiles()).isEmpty();
    }
}
