package util;

import java.util.concurrent.locks.ReentrantLock;

// game state that manages shared state with mutex locking
public class GameState {

    // mutex lock for synchronizing access to game state
    private final ReentrantLock stateLock = new ReentrantLock();
    private int turnCounter = 0;

    // runs an action with the game state locked
    public void withLockedState(Runnable action) {
        stateLock.lock();
        try {
            action.run();
        } finally {
            stateLock.unlock();
        }
    }

    // increments the turn counter
    public void incrementTurnCounter() {
        stateLock.lock();
        try {
            turnCounter++;
        } finally {
            stateLock.unlock();
        }
    }

    // gets the current turn counter
    public int getTurnCounter() {
        stateLock.lock();
        try {
            return turnCounter;
        } finally {
            stateLock.unlock();
        }
    }
}
