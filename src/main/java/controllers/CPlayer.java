package controllers;

import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import models.CDominoes;

public class CPlayer{
    public CPlayer(){};

    public void definePlayerMovement(CDominoes domino, Pane overlay, StackPane hitbox, HBox strip){
        final double[] dragDomino = new double[2];
        final int[] originalIndex = new int[1];

        // prevents key from triggering on every domino in hand
        if (hitbox.getProperties().putIfAbsent("rot-handler", Boolean.TRUE) == null) {
            hitbox.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
                // only react when space or r is released, this tile is on the overlay and it has focus
                if ((e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.R)
                        && hitbox.getParent() == overlay && hitbox.isFocused()) {
                    // rotates domino properties
                    CDominoes.rotateDomino(domino);
                    // rotates image on view
                    ((ImageView) hitbox.getChildren().get(0)).setRotate(domino.getRotationDegrees());
                    e.consume();

                    System.out.println(domino.getTopValue());
                }
            });
        }

        // sets index to whichever domino index is clicked with mouse
        hitbox.setOnMousePressed(e -> {
            originalIndex[0] = strip.getChildren().indexOf(hitbox);

            // gets the position of clicked domino in the scene
            var sceneBounds = hitbox.localToScene(hitbox.getBoundsInLocal());
            // changes that position to the overlay
            var table = overlay.sceneToLocal(sceneBounds.getMinX(), sceneBounds.getMinY());

            // temporarily removes domino from player side
            strip.getChildren().remove(hitbox);
            // allows domino to move over the table via the overlay
            overlay.getChildren().add(hitbox);
            // sets domino coords to where it is on the table
            hitbox.setLayoutX(table.getX());
            hitbox.setLayoutY(table.getY());
            // allows domino to be seen over everything
            hitbox.toFront();

            // allows domino to take key focus
            hitbox.setFocusTraversable(true);
            hitbox.requestFocus();

            // sets where domino is so that it can be dragged with the mouse pointer
            dragDomino[0] = e.getSceneX() - sceneBounds.getMinX();
            dragDomino[1] = e.getSceneY() - sceneBounds.getMinY();
        });

        // checks where domino is each tick and moves it with the mouse
        hitbox.setOnMouseDragged(e -> {
            var q = overlay.sceneToLocal(e.getSceneX(), e.getSceneY());
            hitbox.setLayoutX(q.getX() - dragDomino[0]);
            hitbox.setLayoutY(q.getY() - dragDomino[1]);
        });

        // on release, return domino to the player strip
        hitbox.setOnMouseReleased(e -> {
            overlay.getChildren().remove(hitbox);
            int idx = originalIndex[0];
            if (idx < 0) idx = strip.getChildren().size();
            if (idx > strip.getChildren().size()) idx = strip.getChildren().size();
            strip.getChildren().add(idx, hitbox);

            hitbox.setLayoutX(0);
            hitbox.setLayoutY(0);

            // drop focus once it’s back in the strip so it doesn't trip anything
            hitbox.setFocusTraversable(false);

            // always snap back to vertical-up with minimal code
            int guard = 0;
            while (!"VerticalUp".equals(domino.getOrientation()) && guard++ < 3) {
                CDominoes.rotateDomino(domino);
            }
            ((ImageView) hitbox.getChildren().get(0)).setRotate(domino.getRotationDegrees()); // will be 0
        });


    }
}
