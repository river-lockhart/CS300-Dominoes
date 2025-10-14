package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AvailablePieces {

    private final ArrayList<CDominoes> leftoverDominoes = new ArrayList<>();
    private final Random randomSource = new Random();

    // creates empty boneyard tiles list
    public AvailablePieces() {}

    // creates boneyard from given tiles list
    public AvailablePieces(List<CDominoes> startTiles) {
        if (startTiles != null) leftoverDominoes.addAll(startTiles);
    }

    // creates boneyard from hand leftovers
    public AvailablePieces(Hand hand) {
        if (hand != null) {
            List<CDominoes> handLeftovers = hand.leftoverDominoes();
            if (handLeftovers != null) leftoverDominoes.addAll(handLeftovers);
        }
    }

    // returns live list for direct use
    public ArrayList<CDominoes> getLeftoverDominoes() {
        return leftoverDominoes;
    }

    // returns unchangeable view of leftovers
    public List<CDominoes> view() {
        return Collections.unmodifiableList(leftoverDominoes);
    }

    // returns number of leftover tiles
    public int size() { return leftoverDominoes.size(); }

    // returns if no leftover tiles remain
    public boolean isEmpty() { return leftoverDominoes.isEmpty(); }

    // removes and returns random tile or null
    // logs size before and after removal
    public CDominoes drawRandom() {
        if (leftoverDominoes.isEmpty()) {
            System.out.println("[boneyard] drawRandom: EMPTY");
            return null;
        }
        int sizeBefore = leftoverDominoes.size();
        int pickIndex = randomSource.nextInt(leftoverDominoes.size());
        CDominoes pickedTile = leftoverDominoes.remove(pickIndex);
        int sizeAfter = leftoverDominoes.size();
        return pickedTile;
    }

    // adds tile back into leftovers
    public void putBack(CDominoes tile) {
        if (tile != null) {
            leftoverDominoes.add(tile);
        }
    }
}
