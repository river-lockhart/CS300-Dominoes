package models;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class CRandom {
    // create list to hold dominoes
    private ArrayList<CDominoes> dominoes;

    // constructor fills list with list from CDominoes and shuffles it
    public CRandom() {
        this.dominoes = CDominoes.createGameDominoes();
        Collections.shuffle(this.dominoes);
    }

    // creates a list of 10 random dominoes which will be given to both players in Hand.java
    public ArrayList<CDominoes> dealHand() {
        // list to hold players hand
        int handSize = 10;
        ArrayList<CDominoes> playersHand = new ArrayList<>(handSize);
        
        // adds 10 shuffled dominoes to the hand
        for (int i = 0; i < handSize && !dominoes.isEmpty(); i++) {
            playersHand.add(dominoes.remove(dominoes.size() - 1));
        }
        return playersHand;
    }

    // returns a list of remaining pieces to be displayed
    public ArrayList<CDominoes> getRemainingPieces(){
        ArrayList<CDominoes> remaining = new ArrayList<>(dominoes);

        remaining.sort(Comparator.comparing(CDominoes::getLeftValue).thenComparing(CDominoes::getRightValue));

        return remaining;
    }

}
