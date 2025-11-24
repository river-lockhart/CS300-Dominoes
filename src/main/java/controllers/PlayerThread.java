package controllers;

import util.GameState;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

// player thread that runs player tasks on a dedicated thread
public class PlayerThread {

    private final GameState gameState;
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // builds player thread with needed references
    public PlayerThread(GameState gameState) {
        this.gameState = gameState;
        // single dedicated worker thread for the player
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PlayerThread");
            t.setDaemon(true);
            return t;
        });
    }

    // marks the player thread as active
    public void start() {
        running.set(true);
    }

    // asks the player thread to stop accepting new work
    public void requestStop() {
        running.set(false);
        executor.shutdownNow();
    }

    // submit a task to run on the player thread
    public void submit(Runnable task) {
        if (!running.get()) {
            // quietly ignore if not running
            return;
        }

        if (gameState != null) {
            executor.submit(() -> gameState.withLockedState(task));
        } else {
            executor.submit(task);
        }
    }
}
