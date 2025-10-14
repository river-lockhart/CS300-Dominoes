package controllers;

import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import models.CDominoes;
import models.TableLayout;

import java.util.function.Consumer;

public class CPlayer {
    public CPlayer(){}

    public void definePlayerMovement(CDominoes domino,
                                     Pane overlay,
                                     StackPane hitbox,
                                     HBox strip,
                                     TableLayout tableLayout) {
        final double[] dragDomino = new double[2];
        final int[] originalIndex = new int[1];

        // one-time rotate handler
        if (hitbox.getProperties().putIfAbsent("rot-handler", Boolean.TRUE) == null) {
            hitbox.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
                if ((e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.R)
                        && hitbox.getParent() == overlay && hitbox.isFocused()) {
                    CDominoes.rotateDomino(domino);
                    ((ImageView) hitbox.getChildren().get(0)).setRotate(domino.getRotationDegrees());
                    e.consume();
                }
            });
        }

        hitbox.setOnMousePressed(e -> {
            originalIndex[0] = strip.getChildren().indexOf(hitbox);

            var sceneBounds = hitbox.localToScene(hitbox.getBoundsInLocal());
            var table = overlay.sceneToLocal(sceneBounds.getMinX(), sceneBounds.getMinY());

            strip.getChildren().remove(hitbox);
            overlay.getChildren().add(hitbox);
            hitbox.setLayoutX(table.getX());
            hitbox.setLayoutY(table.getY());
            hitbox.toFront();

            hitbox.setFocusTraversable(true);
            hitbox.requestFocus();

            dragDomino[0] = e.getSceneX() - sceneBounds.getMinX();
            dragDomino[1] = e.getSceneY() - sceneBounds.getMinY();
        });

        hitbox.setOnMouseDragged(e -> {
            var q = overlay.sceneToLocal(e.getSceneX(), e.getSceneY());
            hitbox.setLayoutX(q.getX() - dragDomino[0]);
            hitbox.setLayoutY(q.getY() - dragDomino[1]);
        });

        hitbox.setOnMouseReleased(e -> {
            boolean placed = false;
            if (tableLayout != null) {
                placed = tableLayout.tryPlaceOnGrid(domino, hitbox, strip, "Player");
            }

            if (placed) {
                // successful commit: if an onCommit callback was attached to this tile,
                // call it so the model hand can be updated and auto-draw can run.
                @SuppressWarnings("unchecked")
                Consumer<CDominoes> onCommit = (Consumer<CDominoes>) hitbox.getProperties().get("onCommit");
                if (onCommit != null) {
                    try {
                        onCommit.accept(domino);
                    } catch (Exception ex) {
                        System.out.println("[place] onCommit threw: " + ex);
                    }
                }
                return;
            }

            overlay.getChildren().remove(hitbox);
            int idx = originalIndex[0];
            if (idx < 0) idx = strip.getChildren().size();
            if (idx > strip.getChildren().size()) idx = strip.getChildren().size();
            strip.getChildren().add(idx, hitbox);

            hitbox.setLayoutX(0);
            hitbox.setLayoutY(0);

            hitbox.setFocusTraversable(false);

            int guard = 0;
            while (!"VerticalUp".equals(domino.getOrientation()) && guard++ < 3) {
                CDominoes.rotateDomino(domino);
            }
            ((ImageView) hitbox.getChildren().get(0)).setRotate(domino.getRotationDegrees());
        });
    }
}
