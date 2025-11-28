package com.trionix.maps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TileCacheBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void buildMemoryOnlyCacheReturnsInMemoryTileCache() {
        TileCache cache = TileCacheBuilder.create()
                .memory(500)
                .build();

        assertThat(cache).isInstanceOf(InMemoryTileCache.class);
    }

    @Test
    void buildDiskOnlyCacheReturnsFileTileCache() {
        TileCache cache = TileCacheBuilder.create()
                .disk(tempDir.resolve("tiles"), 10000)
                .build();

        assertThat(cache).isInstanceOf(FileTileCache.class);
    }

    @Test
    void buildTieredCacheReturnsTieredTileCache() {
        TileCache cache = TileCacheBuilder.create()
                .memory(500)
                .disk(tempDir.resolve("tiles"), 10000)
                .build();

        assertThat(cache).isInstanceOf(TieredTileCache.class);
    }

    @Test
    void buildWithNoConfigurationThrowsIllegalStateException() {
        assertThatThrownBy(() -> TileCacheBuilder.create().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No cache tiers configured");
    }

    @Test
    void builderCanChainMultipleMemoryCaches() {
        TileCache cache = TileCacheBuilder.create()
                .memory(100)
                .memory(200)
                .build();

        assertThat(cache).isInstanceOf(TieredTileCache.class);
    }

    @Test
    void builderCanChainMultipleDiskCaches() {
        TileCache cache = TileCacheBuilder.create()
                .disk(tempDir.resolve("tiles1"), 5000)
                .disk(tempDir.resolve("tiles2"), 10000)
                .build();

        assertThat(cache).isInstanceOf(TieredTileCache.class);
    }
}
