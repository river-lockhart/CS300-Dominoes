package models;

import java.util.ArrayList;

public class Hand{
    // splits the game hand into three sections
    private ArrayList<CDominoes> playerHand;
    private ArrayList<CDominoes> aiHand;
    private ArrayList<CDominoes> remainingPieces;

    public Hand(){
        // creates a pool for the current deck in the game
        CRandom pool = new CRandom();
        this.playerHand = pool.dealHand();
        this.aiHand = pool.dealHand();
        this.remainingPieces = pool.getRemainingPieces();
    }

    public ArrayList<CDominoes> getPlayerHand(){
        return this.playerHand;
    }

    public ArrayList<CDominoes> getAiHand(){
        return this.aiHand;
    }

    public int getPlayerCount(){
        return playerHand.size();
    }

    public int getAiCount(){
        return aiHand.size();
    }

    public ArrayList<CDominoes> leftoverDominoes(){
        return this.remainingPieces;
    }
}