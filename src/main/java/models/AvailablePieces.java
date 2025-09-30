package models;

import java.util.ArrayList;

public class AvailablePieces{
    private final Hand deck;

    // associate available piece object with the current game deck
    public AvailablePieces(Hand deck){
        this.deck = deck;
    }

    // retrieve the leftover domino pieces from game specific deck
    public ArrayList<CDominoes> getLeftoverDominoes(){
        return deck.leftoverDominoes();
    }



}