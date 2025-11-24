package controllers;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.application.Platform;
import util.GameState;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TurnManager {
    public enum Side { PLAYER, AI }

    // holds the value for both sides
    private final ObjectProperty<Side> turn = new SimpleObjectProperty<>();

    // executor service for running tasks concurrently
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private GameState gameState = new GameState();

    public TurnManager() {
        coinFlipStart();
    }

    // builds turn manager with needed references
    public TurnManager(GameState gameState) {
        this.gameState = gameState;
        coinFlipStart();
    }

    // sets the game state reference
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    // 50/50 chance for either player to start first
    public void coinFlipStart() {
        turn.set(Math.random() < 0.5 ? Side.PLAYER : Side.AI);
    }

    // for binding property
    public ObjectProperty<Side> turnProperty() {
        return turn;
    }

    // for retrieving side value
    public Side getTurn() {
        return turn.get();
    }

    // toggles between player turns
    public void next() {
        turn.set(getTurn() == Side.PLAYER ? Side.AI : Side.PLAYER);
        if (gameState != null) {
            gameState.incrementTurnCounter();
        }
    }

    // return boolean value for player turn
    public boolean isPlayerTurn() {
        return getTurn() == Side.PLAYER;
    }

    public void runAiTurn(AIPlayer aiPlayer) {
        executor.submit(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            Platform.runLater(() -> {
                // only act if it is still the ai's turn
                if (getTurn() == Side.AI) {
                    aiPlayer.performInstantTurn();
                }
            });
        });
    }

    // shuts down the executor service
    public void shutdown() {
        executor.shutdown();
    }
}
