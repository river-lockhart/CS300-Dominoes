package controllers;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import models.CDominoes;
import models.Hand;
import models.TableLayout;
import models.AvailablePieces;
import util.ConsoleLogger;

import java.util.Optional;

public class AIPlayer extends CPlayer {

    private final TableLayout tableLayout;
    private final TurnManager turnManager;
    private final HBox computerHandRow;

    private final Hand hand;
    private final AvailablePieces boneyard;

    private PauseTransition waitTimer;

    // builds ai player with needed references
    public AIPlayer(TableLayout tableLayout, TurnManager turnManager, HBox aiStrip, Hand hand, AvailablePieces boneyard) {
        this.tableLayout = tableLayout;
        this.turnManager = turnManager;
        this.computerHandRow = aiStrip;
        this.hand = hand;
        this.boneyard = boneyard;
    }

    // schedules ai move after a short delay
    public void takeTurnWithDelay() {
        if (waitTimer != null) {
            waitTimer.stop();
            waitTimer = null;
        }
        waitTimer = new PauseTransition(Duration.seconds(3));
        waitTimer.setOnFinished(e -> {
            waitTimer = null;
            doAIMove();
        });
        waitTimer.play();
    }

    // chooses to place, draw, or pass
    private void doAIMove() {
        if (turnManager.getTurn() != TurnManager.Side.AI) return;

        if (hasAnyPlayable()) {
            placeFirstLegal();
            return;
        }

        boolean didDraw = drawOneIntoAIHand();
        if (didDraw) {
            if (waitTimer != null) {
                waitTimer.stop();
            }
            waitTimer = new PauseTransition(Duration.seconds(1.5));
            waitTimer.setOnFinished(e -> {
                waitTimer = null;
                doAIMove();
            });
            waitTimer.play();
            return;
        }

        ConsoleLogger.logPass(TurnManager.Side.AI);
        turnManager.next();
    }

    // checks if any tile can be placed
    private boolean hasAnyPlayable() {
        for (int index = 0; index < computerHandRow.getChildren().size(); index++) {
            StackPane tileBox = (StackPane) computerHandRow.getChildren().get(index);
            Object modelObject = tileBox.getProperties().get("model");
            if (!(modelObject instanceof CDominoes)) continue;
            CDominoes domino = (CDominoes) modelObject;

            String startFacing = domino.getOrientation();
            Optional<TableLayout.Placement> placementOption = tableLayout.findLegalPlacementAnywhere(domino);
            int rotateLimit = 0;
            while (!startFacing.equals(domino.getOrientation()) && rotateLimit++ < 4) {
                CDominoes.rotateDomino(domino);
            }
            if (placementOption.isPresent()) return true;
        }
        return false;
    }

    // places the first legal tile found
    private void placeFirstLegal() {
        for (int index = 0; index < computerHandRow.getChildren().size(); index++) {
            StackPane tileBox = (StackPane) computerHandRow.getChildren().get(index);
            Object modelObject = tileBox.getProperties().get("model");
            if (!(modelObject instanceof CDominoes)) continue;
            CDominoes domino = (CDominoes) modelObject;

            Optional<TableLayout.Placement> placementOption = tableLayout.findLegalPlacementAnywhere(domino);
            if (placementOption.isEmpty()) continue;

            TableLayout.Placement plan = placementOption.get();

            boolean wantVertical = plan.vertical;
            boolean isVertical = domino.getOrientation().startsWith("Vertical");
            int rotateLimit = 0;
            while (isVertical != wantVertical && rotateLimit++ < 4) {
                CDominoes.rotateDomino(domino);
                isVertical = domino.getOrientation().startsWith("Vertical");
            }

            if (!tileBox.getChildren().isEmpty() && tileBox.getChildren().get(0) instanceof ImageView) {
                ((ImageView) tileBox.getChildren().get(0)).setRotate(domino.getRotationDegrees());
            }

            hand.getAiHand().remove(domino);

            tableLayout.commitPlacementAt(domino, tileBox, computerHandRow, plan);
            return;
        }

        turnManager.next();
    }

    // draws one tile into the ai hand
    private boolean drawOneIntoAIHand() {
        if (boneyard == null || boneyard.isEmpty()) {
            return false;
        }

        CDominoes drawn = boneyard.drawRandom();
        if (drawn == null) {
            return false;
        }

        hand.getAiHand().add(drawn);
        ConsoleLogger.logDraw(TurnManager.Side.AI, drawn);

        var imageLink = getClass().getResource(drawn.getImage());
        if (imageLink != null) {
            Image image = new Image(imageLink.toExternalForm(), true);
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setCache(true);

            StackPane parentBar = (StackPane) computerHandRow.getParent();
            if (parentBar != null) {
                imageView.fitHeightProperty().bind(parentBar.heightProperty().subtract(12));
            } else {
                Platform.runLater(() -> {
                    StackPane parentPane = (StackPane) computerHandRow.getParent();
                    if (parentPane != null) imageView.fitHeightProperty().bind(parentPane.heightProperty().subtract(12));
                });
            }
            imageView.setRotate(drawn.getRotationDegrees());

            StackPane tileSlot = new StackPane(imageView);
            tileSlot.setPadding(new javafx.geometry.Insets(4));
            HBox.setHgrow(tileSlot, javafx.scene.layout.Priority.NEVER);

            tileSlot.getProperties().put("model", drawn);
            computerHandRow.getChildren().add(tileSlot);
        }

        return true;
    }
}
