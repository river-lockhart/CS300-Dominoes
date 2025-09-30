package controllers;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class CPlayer{

    
    public CPlayer(){};

    public void definePlayerMovement(Pane overlay, StackPane hitbox, HBox strip){
        final double[] dragDomino = new double[2];
                final int[] originalIndex = new int[1];

                hitbox.setOnMousePressed(e -> {
                    originalIndex[0] = strip.getChildren().indexOf(hitbox);

                    var sceneBounds = hitbox.localToScene(hitbox.getBoundsInLocal());
                    var p = overlay.sceneToLocal(sceneBounds.getMinX(), sceneBounds.getMinY());

                    strip.getChildren().remove(hitbox);
                    overlay.getChildren().add(hitbox);
                    hitbox.setLayoutX(p.getX());
                    hitbox.setLayoutY(p.getY());
                    hitbox.toFront();

                    dragDomino[0] = e.getSceneX() - sceneBounds.getMinX();
                    dragDomino[1] = e.getSceneY() - sceneBounds.getMinY();
                });

                hitbox.setOnMouseDragged(e -> {
                    var q = overlay.sceneToLocal(e.getSceneX(), e.getSceneY());
                    hitbox.setLayoutX(q.getX() - dragDomino[0]);
                    hitbox.setLayoutY(q.getY() - dragDomino[1]);
                });

                hitbox.setOnMouseReleased(e -> {
                    overlay.getChildren().remove(hitbox);
                    strip.getChildren().add(originalIndex[0], hitbox);
                    hitbox.setLayoutX(0);
                    hitbox.setLayoutY(0);
                });
    }

}