package com.trionix.maps;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javax.imageio.ImageIO;

/**
 * Disk-based tile cache using file system storage with OSM-style directory structure.
 *
 * <p>Tiles are stored as PNG files at {@code {cacheDir}/{zoom}/{x}/{y}.png}.
 * LRU eviction is performed based on file last-modified time when the cache exceeds capacity.
 *
 * <p>This implementation is thread-safe. Concurrent access is protected via locking
 * and atomic file operations (write to temp file, then move).
 */
public final class FileTileCache implements TileCache {

    private final Path cacheDir;
    private final int maxFiles;
    private final ReentrantLock evictionLock = new ReentrantLock();

    /**
     * Creates a disk-based tile cache.
     *
     * @param cacheDir the directory where tiles are stored
     * @param maxFiles the maximum number of tile files to store (must be positive)
     * @throws IllegalArgumentException if cacheDir is null or maxFiles is not positive
     */
    public FileTileCache(Path cacheDir, int maxFiles) {
        if (cacheDir == null) {
            throw new IllegalArgumentException("cacheDir must not be null");
        }
        if (maxFiles <= 0) {
            throw new IllegalArgumentException("maxFiles must be positive");
        }
        this.cacheDir = cacheDir;
        this.maxFiles = maxFiles;
    }

    @Override
    public Image get(int zoom, long x, long y) {
        Path tilePath = tilePath(zoom, x, y);
        if (!Files.exists(tilePath)) {
            return null;
        }
        try {
            // Touch file for LRU tracking
            Files.setLastModifiedTime(tilePath, FileTime.fromMillis(System.currentTimeMillis()));
            return new Image(tilePath.toUri().toString());
        } catch (IOException e) {
            // File may have been deleted by concurrent eviction
            return null;
        }
    }

    @Override
    public void put(int zoom, long x, long y, Image image) {
        Objects.requireNonNull(image, "image");
        Path tilePath = tilePath(zoom, x, y);
        try {
            Files.createDirectories(tilePath.getParent());
            // Atomic write: write to temp file, then move
            Path tempFile = Files.createTempFile(tilePath.getParent(), "tile", ".tmp");
            try {
                var bufferedImage = SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(bufferedImage, "png", tempFile.toFile());
                Files.move(tempFile, tilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.deleteIfExists(tempFile);
                throw e;
            }
            evictIfNeeded();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write tile to disk", e);
        }
    }

    @Override
    public void clear() {
        if (!Files.exists(cacheDir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(cacheDir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Best effort deletion
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to clear cache directory", e);
        }
    }

    private Path tilePath(int zoom, long x, long y) {
        return cacheDir.resolve(String.valueOf(zoom))
                .resolve(String.valueOf(x))
                .resolve(y + ".png");
    }

    private void evictIfNeeded() {
        if (!evictionLock.tryLock()) {
            // Another thread is already evicting
            return;
        }
        try {
            if (!Files.exists(cacheDir)) {
                return;
            }
            try (Stream<Path> files = Files.walk(cacheDir)) {
                var tileFiles = files
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".png"))
                        .toList();

                if (tileFiles.size() <= maxFiles) {
                    return;
                }

                // Sort by last modified time (oldest first) and delete excess
                int toDelete = tileFiles.size() - maxFiles;
                tileFiles.stream()
                        .sorted(Comparator.comparing(this::getLastModifiedTime))
                        .limit(toDelete)
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                // Best effort deletion
                            }
                        });
            }
        } catch (IOException e) {
            // Best effort eviction - don't fail the put operation
        } finally {
            evictionLock.unlock();
        }
    }

    private FileTime getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            return FileTime.fromMillis(0);
        }
    }
}
