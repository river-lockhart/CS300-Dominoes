package models;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

public class CDominoes{

    // properties of dominoes
    private String orientation;
    private String image;
    private Integer leftValue;
    private Integer rightValue;
    private Integer topValue;
    private Integer bottomValue;
    private int rotationDegrees = 0;

    // domino constructor
    public CDominoes(String orientation, String image, Integer leftValue, Integer rightValue, Integer topValue, Integer bottomValue){
        this.orientation = orientation;
        this.image = image;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        this.topValue = topValue;
        this.bottomValue = bottomValue;
    }

    // creates an arraylist with all 28 dominoes
    public static ArrayList<CDominoes> createGameDominoes() {
        ArrayList<CDominoes> dominoes = new ArrayList<>();
        try {
            // retrieve resource folder
            URL resourceFolder = CDominoes.class.getResource("/assets/dominoImages/");
            if (resourceFolder == null) return dominoes;
            // gets each domino image from the resource folder
            File folder = new File(resourceFolder.toURI());
            File[] files = folder.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                // iterates through each domino image and splits to find domino values
                for (File f : files) {
                    // retrieves image path
                    String imagePath = "/assets/dominoImages/" + f.getName();
                    // removes extraneous text
                    String name = f.getName().replace("Domino-", "").replace(".PNG","").replace(".png","");
                    // splits at the "." between values
                    String[] parts = name.split("\\.");
                    // stores domino number values
                    int top = Integer.parseInt(parts[0]);
                    int bottom = Integer.parseInt(parts[1]);

                    // creates domino object with info from image name and default Vertical orientation
                    dominoes.add(new CDominoes(
                        "Vertical", 
                        imagePath, 
                        null, 
                        null, 
                        top, 
                        bottom));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return dominoes;
    }

    // function to rotate domino image and value between vertical/horizontal and vice versa
    public static void rotateDomino(CDominoes domino){
        // swaps orientation
        if((domino.orientation).equals("Vertical")){
            domino.orientation = "Horizontal";
            // swaps value orientation
            domino.leftValue = domino.topValue;
            domino.topValue = null;
            domino.rightValue = domino.bottomValue;
            domino.bottomValue = null;
            // value to rotate image holder in ui
            domino.rotationDegrees = -90;
        }else{
            domino.orientation = "Vertical";
            domino.topValue = domino.leftValue;
            domino.leftValue = null;
            domino.bottomValue = domino.rightValue;
            domino.rightValue = null;
            domino.rotationDegrees = 0;
        }
    }
    
    // getters
    public String  getOrientation()     { return orientation; }
    public String  getImage()           { return image; }
    public Integer getLeftValue()       { return leftValue; }
    public Integer getRightValue()      { return rightValue; }
    public Integer getTopValue()        { return topValue; }
    public Integer getBottomValue()     { return bottomValue; }
    public int getRotationDegrees()     { return rotationDegrees; }
}
