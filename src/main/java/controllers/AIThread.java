package controllers;

import javafx.application.Platform;

//ai thread that watches the turn manager and triggers ai moves
public class AIThread extends Thread {

    private final TurnManager turnManager;
    private final AIPlayer aiPlayer;

    // flag to control the main loop
    private volatile boolean running = true;

    // how often we poll for turn changes (in ms)
    private static final long TURN_POLL_SLEEP_MS = 50L;

    public AIThread(TurnManager turnManager, AIPlayer aiPlayer) {
        // force thread name
        super("AIThread");
        // save references
        this.turnManager = turnManager;
        this.aiPlayer = aiPlayer;
        // make daemon so it won't block app exit
        setDaemon(true); 
    }

    // asks the thread to stop
    public void requestStop() {
        running = false;
        interrupt();
    }

    @Override
    public void run() {
        while (running) {
            try {
                // wait until it's the ai's turn
                if (turnManager.getTurn() == TurnManager.Side.AI) {
                    // schedule the ai move on the javafx application thread
                    Platform.runLater(() -> {
                        try {
                            aiPlayer.takeTurnWithDelay();
                        } catch (Exception ex) {
                            System.out.println("[ai-thread] aiPlayer.takeTurnWithDelay threw: " + ex);
                        }
                    });

                    // wait for turn to switch away from ai before looping again
                    waitForTurnToChangeFromAI();
                } else {
                    // sleep briefly before polling again
                    Thread.sleep(TURN_POLL_SLEEP_MS);
                }
            } catch (InterruptedException ex) {
                if (!running) {
                    // stops on purpose
                    break;
                }
            } catch (Exception ex) {
                System.out.println("[ai-thread] unexpected exception in run loop: " + ex);
            }
        }
    }

    // waits until the turn manager says it's no longer the ai's turn
    private void waitForTurnToChangeFromAI() throws InterruptedException {
        while (running && turnManager.getTurn() == TurnManager.Side.AI) {
            Thread.sleep(TURN_POLL_SLEEP_MS);
        }
    }
}
