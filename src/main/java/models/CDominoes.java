package models;

import java.net.URL;
import java.util.ArrayList;

public class CDominoes {

    // properties of dominoes
    private String orientation;
    private String image;
    private Integer leftValue;
    private Integer rightValue;
    private Integer topValue;
    private Integer bottomValue;
    private int rotationDegrees = 0;

    // domino constructor
    public CDominoes(String orientation, String image,
                     Integer leftValue, Integer rightValue,
                     Integer topValue, Integer bottomValue) {
        this.orientation = orientation;
        this.image = image;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        this.topValue = topValue;
        this.bottomValue = bottomValue;
    }

    // creates an arraylist with all 28 dominoes (double-six set)
    public static ArrayList<CDominoes> createGameDominoes() {
        ArrayList<CDominoes> dominoes = new ArrayList<>();

        // standard double-six set: 0-0, 0-1, ... , 6-6 (i <= j)
        for (int top = 0; top <= 6; top++) {
            for (int bottom = top; bottom <= 6; bottom++) {

                // adjust this pattern to match your actual filenames
                String lower = String.format("/assets/dominoImages/Domino-%d.%d.png", top, bottom);
                String upper = String.format("/assets/dominoImages/Domino-%d.%d.PNG", top, bottom);

                URL img = CDominoes.class.getResource(lower);
                String imagePath;

                if (img != null) {
                    imagePath = lower;
                } else {
                    img = CDominoes.class.getResource(upper);
                    if (img != null) {
                        imagePath = upper;
                    } else {
                        System.err.println("WARNING: missing domino image for " + top + "." + bottom);
                        continue; // skip if we really don't have this file
                    }
                }

                dominoes.add(new CDominoes(
                        "VerticalUp",
                        imagePath,
                        null,
                        null,
                        top,
                        bottom
                ));
            }
        }

        System.out.println("DEBUG: created " + dominoes.size() + " dominoes.");
        return dominoes;
    }

    // function to rotate domino image and value between vertical/horizontal/upside down and vice versa
    public static void rotateDomino(CDominoes domino) {
        if ("VerticalUp".equals(domino.orientation)) {
            domino.orientation = "HorizontalLeft";
            domino.leftValue = domino.topValue;
            domino.rightValue = domino.bottomValue;
            domino.topValue = null;
            domino.bottomValue = null;
            domino.rotationDegrees = -90;
        } else if ("HorizontalLeft".equals(domino.orientation)) {
            domino.orientation = "VerticalDown";
            domino.topValue = domino.rightValue;
            domino.bottomValue = domino.leftValue;
            domino.leftValue = null;
            domino.rightValue = null;
            domino.rotationDegrees = -180;
        } else if ("VerticalDown".equals(domino.orientation)) {
            domino.orientation = "HorizontalRight";
            domino.leftValue = domino.topValue;
            domino.rightValue = domino.bottomValue;
            domino.topValue = null;
            domino.bottomValue = null;
            domino.rotationDegrees = -270;
        } else {
            domino.orientation = "VerticalUp";
            domino.topValue = domino.rightValue;
            domino.bottomValue = domino.leftValue;
            domino.leftValue = null;
            domino.rightValue = null;
            domino.rotationDegrees = 0;
        }
    }

    // getters
    public String  getOrientation()    { return orientation; }
    public String  getImage()          { return image; }
    public Integer getLeftValue()      { return leftValue; }
    public Integer getRightValue()     { return rightValue; }
    public Integer getTopValue()       { return topValue; }
    public Integer getBottomValue()    { return bottomValue; }
    public int     getRotationDegrees(){ return rotationDegrees; }
}
