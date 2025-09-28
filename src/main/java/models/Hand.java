package models;

import java.util.ArrayList;

public class Hand{
    private ArrayList<CDominoes> playerHand;
    private ArrayList<CDominoes> aiHand;
    private ArrayList<CDominoes> remainingPieces;

    public Hand(){
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