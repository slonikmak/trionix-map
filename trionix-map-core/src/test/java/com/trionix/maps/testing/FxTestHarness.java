package com.trionix.maps.testing;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;

/** Utility to ensure the JavaFX Platform is initialized for headless tests. */
public final class FxTestHarness {

    static {
        initializeToolkit();
    }

    private FxTestHarness() {
    }

    public static void runOnFxThread(Runnable runnable) {
        FutureTask<Void> task = new FutureTask<>(runnable, null);
        Platform.runLater(task);
        waitFor(task);
    }

    public static <T> T callOnFxThread(Callable<T> callable) {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return waitFor(task);
    }

    private static void initializeToolkit() {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyRunning) {
            Platform.runLater(latch::countDown);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting JavaFX platform", e);
        }
    }

    private static <T> T waitFor(FutureTask<T> task) {
        try {
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for FX task", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("FX task failed", e.getCause());
        }
    }
}
