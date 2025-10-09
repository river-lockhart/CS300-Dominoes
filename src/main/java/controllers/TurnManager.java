package controllers;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class TurnManager {
    public enum Side { PLAYER, AI }

    // holds the value for both sides
    private final ObjectProperty<Side> turn = new SimpleObjectProperty<>();

    public TurnManager() { 
        coinFlipStart(); 
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
    }

    // return boolean value for player turn
    public boolean isPlayerTurn() { 
        return getTurn() == Side.PLAYER; 
    }
}
