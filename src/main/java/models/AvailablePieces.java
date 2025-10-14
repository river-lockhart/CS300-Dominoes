package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Boneyard / remaining tiles after the initial deal.
 * Overlay (and draw-until-playable) read from here.
 */
public class AvailablePieces {

    private final ArrayList<CDominoes> leftoverDominoes = new ArrayList<>();
    private final Random rng = new Random();

    public AvailablePieces() {}

    /** Construct from an explicit list of leftovers. */
    public AvailablePieces(List<CDominoes> seed) {
        if (seed != null) leftoverDominoes.addAll(seed);
    }

    /**
     * Back-compat: construct from Hand.
     * IMPORTANT: change the method name below to whatever your Hand uses
     * for "the deck tiles that were NOT dealt".
     */
    public AvailablePieces(Hand hand) {
        if (hand != null) {
            // ⬇️ CHANGE THIS if your Hand uses a different name
            List<CDominoes> leftoversFromHand = hand.leftoverDominoes();
            if (leftoversFromHand != null) leftoverDominoes.addAll(leftoversFromHand);
        }
    }

    /** Live list (mutable). Overlay uses this via the supplier. */
    public ArrayList<CDominoes> getLeftoverDominoes() {
        return leftoverDominoes;
    }

    /** Safe read-only view. */
    public List<CDominoes> view() {
        return Collections.unmodifiableList(leftoverDominoes);
    }

    public int size() { return leftoverDominoes.size(); }
    public boolean isEmpty() { return leftoverDominoes.isEmpty(); }

    /** Removes and returns a random tile, or null if empty. */
    public CDominoes drawRandom() {
        if (leftoverDominoes.isEmpty()) {
            System.out.println("[boneyard] drawRandom: EMPTY");
            return null;
        }
        int before = leftoverDominoes.size();
        int idx = rng.nextInt(leftoverDominoes.size());
        CDominoes picked = leftoverDominoes.remove(idx);
        int after = leftoverDominoes.size();
        System.out.println("[boneyard] removed idx=" + idx + " tile=" + picked
                + " | size " + before + " -> " + after);
        return picked;
    }

    /** Put a tile back (e.g., for undo). */
    public void putBack(CDominoes d) {
        if (d != null) {
            leftoverDominoes.add(d);
            System.out.println("[boneyard] putBack: " + d + " | size -> " + leftoverDominoes.size());
        }
    }
}
