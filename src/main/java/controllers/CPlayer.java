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
    // makes a simple helper for player actions
    public CPlayer(){}

    // sets up player tile dragging and placement behavior
    public void definePlayerMovement(CDominoes domino,
                                     Pane overlay,
                                     StackPane tileBox,
                                     HBox handRow,
                                     TableLayout tableLayout) {
        double[] dragOffset = new double[2];
        int[] startIndex = new int[1];

        // listens for rotate key after tile moves to overlay
        if (tileBox.getProperties().putIfAbsent("rot-handler", Boolean.TRUE) == null) {
            tileBox.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                if ((event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.R)
                        && tileBox.getParent() == overlay && tileBox.isFocused()) {
                    CDominoes.rotateDomino(domino);
                    ((ImageView) tileBox.getChildren().get(0)).setRotate(domino.getRotationDegrees());
                    event.consume();
                }
            });
        }

        // picks up the tile and moves it onto the overlay
        // saves list index so return slot is remembered
        // computes drag offset so the tile does not jump
        tileBox.setOnMousePressed(event -> {
            startIndex[0] = handRow.getChildren().indexOf(tileBox);

            var sceneBounds = tileBox.localToScene(tileBox.getBoundsInLocal());
            var localPoint = overlay.sceneToLocal(sceneBounds.getMinX(), sceneBounds.getMinY());

            handRow.getChildren().remove(tileBox);
            overlay.getChildren().add(tileBox);
            tileBox.setLayoutX(localPoint.getX());
            tileBox.setLayoutY(localPoint.getY());
            tileBox.toFront();

            tileBox.setFocusTraversable(true);
            tileBox.requestFocus();

            dragOffset[0] = event.getSceneX() - sceneBounds.getMinX();
            dragOffset[1] = event.getSceneY() - sceneBounds.getMinY();
        });

        // drags the tile around following the mouse
        tileBox.setOnMouseDragged(event -> {
            var dragPoint = overlay.sceneToLocal(event.getSceneX(), event.getSceneY());
            tileBox.setLayoutX(dragPoint.getX() - dragOffset[0]);
            tileBox.setLayoutY(dragPoint.getY() - dragOffset[1]);
        });

        // tries to place tile on board, or returns it to hand
        // restores default facing when returning to the hand
        // limits rotation attempts to prevent endless spinning
        tileBox.setOnMouseReleased(event -> {
            boolean placed = false;
            if (tableLayout != null) {
                placed = tableLayout.tryPlaceOnGrid(domino, tileBox, handRow, "Player");
            }

            if (placed) {
                @SuppressWarnings("unchecked")
                Consumer<CDominoes> onCommit = (Consumer<CDominoes>) tileBox.getProperties().get("onCommit");
                if (onCommit != null) {
                    try {
                        onCommit.accept(domino);
                    } catch (Exception ex) {
                        System.out.println("[place] onCommit threw: " + ex);
                    }
                }
                return;
            }

            overlay.getChildren().remove(tileBox);
            int insertIndex = startIndex[0];
            if (insertIndex < 0) insertIndex = handRow.getChildren().size();
            if (insertIndex > handRow.getChildren().size()) insertIndex = handRow.getChildren().size();
            handRow.getChildren().add(insertIndex, tileBox);

            tileBox.setLayoutX(0);
            tileBox.setLayoutY(0);

            tileBox.setFocusTraversable(false);

            int rotateLimit = 0;
            while (!"VerticalUp".equals(domino.getOrientation()) && rotateLimit++ < 3) {
                CDominoes.rotateDomino(domino);
            }
            ((ImageView) tileBox.getChildren().get(0)).setRotate(domino.getRotationDegrees());
        });
    }
}
