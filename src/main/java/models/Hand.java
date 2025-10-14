package models;

import java.util.ArrayList;

public class Hand{
    // splits the game hand into three sections
    private ArrayList<CDominoes> playerHand;
    private ArrayList<CDominoes> computerHand;
    private ArrayList<CDominoes> remainingPieces;

    public Hand(){
        // creates a pool for the current deck in the game
        CRandom deckPool = new CRandom();
        this.playerHand = deckPool.dealHand();
        this.computerHand = deckPool.dealHand();
        this.remainingPieces = deckPool.getRemainingPieces();
    }

    // returns the player tiles list
    public ArrayList<CDominoes> getPlayerHand(){
        return this.playerHand;
    }

    // returns the computer tiles list
    public ArrayList<CDominoes> getAiHand(){
        return this.computerHand;
    }

    // returns count of player tiles
    public int getPlayerCount(){
        return playerHand.size();
    }

    // returns count of computer tiles
    public int getAiCount(){
        return computerHand.size();
    }

    // returns tiles not dealt to hands
    public ArrayList<CDominoes> leftoverDominoes(){
        return this.remainingPieces;
    }
}
