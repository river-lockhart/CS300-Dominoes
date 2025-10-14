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

import java.util.Optional;

/**
 * AI flow:
 * - takeTurnWithDelay(): 3s before starting its turn.
 * - doAIMove():
 *     - If it already has a playable tile, place immediately.
 *     - Otherwise, draw ONE tile, then wait 1.5s and try again.
 *     - Repeat until playable or boneyard empty.
 *     - If boneyard empty and still no move, pass.
 *
 * TableLayout advances the turn on commitPlacementAt; we only call next() when passing.
 */
public class AIPlayer extends CPlayer {

    private final TableLayout tableLayout;
    private final TurnManager turnManager;
    private final HBox aiStrip;

    // model + boneyard so AI can draw like the player
    private final Hand hand;
    private final AvailablePieces boneyard;

    private PauseTransition pending;

    public AIPlayer(TableLayout tableLayout,
                    TurnManager turnManager,
                    HBox aiStrip,
                    Hand hand,
                    AvailablePieces boneyard) {
        this.tableLayout = tableLayout;
        this.turnManager = turnManager;
        this.aiStrip = aiStrip;
        this.hand = hand;
        this.boneyard = boneyard;
    }

    /** schedule a move ~3s later to feel more natural */
    public void takeTurnWithDelay() {
        if (pending != null) {
            pending.stop();
            pending = null;
        }
        pending = new PauseTransition(Duration.seconds(3));
        pending.setOnFinished(e -> {
            pending = null;
            doAIMove();
        });
        pending.play();
    }

    private void doAIMove() {
        if (turnManager.getTurn() != TurnManager.Side.AI) return;

        // If a move is already possible, place immediately (no extra pause).
        if (hasAnyPlayable()) {
            placeFirstLegal();
            return;
        }

        // Otherwise, draw ONE tile. Then wait 1.5s before trying again.
        boolean drew = drawOneIntoAIHand();
        if (drew) {
            if (pending != null) {
                pending.stop();
            }
            pending = new PauseTransition(Duration.seconds(1.5));
            pending.setOnFinished(e -> {
                pending = null;
                doAIMove();  // try again after the pause (either draw again or place)
            });
            pending.play();
            return;
        }

        // Boneyard empty and still no legal move -> pass.
        System.out.println("[AI] No legal move after drawing (empty boneyard); passing.");
        turnManager.next();
    }

    /** Check if any AI tile is playable anywhere on the current board. */
    private boolean hasAnyPlayable() {
        for (int i = 0; i < aiStrip.getChildren().size(); i++) {
            StackPane hb = (StackPane) aiStrip.getChildren().get(i);
            Object modelObj = hb.getProperties().get("model");
            if (!(modelObj instanceof CDominoes)) continue;
            CDominoes domino = (CDominoes) modelObj;

            // probe; restore orientation if changed by probe
            String orig = domino.getOrientation();
            Optional<TableLayout.Placement> planOpt = tableLayout.findLegalPlacementAnywhere(domino);
            int guard = 0;
            while (!orig.equals(domino.getOrientation()) && guard++ < 4) {
                CDominoes.rotateDomino(domino);
            }
            if (planOpt.isPresent()) return true;
        }
        return false;
    }

    /** Place the first legal tile found; TableLayout handles advancing the turn. */
    private void placeFirstLegal() {
        for (int i = 0; i < aiStrip.getChildren().size(); i++) {
            StackPane hb = (StackPane) aiStrip.getChildren().get(i);
            Object modelObj = hb.getProperties().get("model");
            if (!(modelObj instanceof CDominoes)) continue;
            CDominoes domino = (CDominoes) modelObj;

            Optional<TableLayout.Placement> planOpt = tableLayout.findLegalPlacementAnywhere(domino);
            if (planOpt.isEmpty()) continue;

            TableLayout.Placement plan = planOpt.get();

            // rotate to match plan orientation
            boolean wantVertical = plan.vertical;
            boolean isVertical = domino.getOrientation().startsWith("Vertical");
            int rGuard = 0;
            while (isVertical != wantVertical && rGuard++ < 4) {
                CDominoes.rotateDomino(domino);
                isVertical = domino.getOrientation().startsWith("Vertical");
            }

            if (!hb.getChildren().isEmpty() && hb.getChildren().get(0) instanceof ImageView) {
                ((ImageView) hb.getChildren().get(0)).setRotate(domino.getRotationDegrees());
            }

            // commit to board (TableLayout will handle advancing the turn)
            tableLayout.commitPlacementAt(domino, hb, aiStrip, plan);

            // remove from the AI model hand (mirrors player removal)
            boolean removed = hand.getAiHand().remove(domino);
            System.out.println("[AI] Placed " + domino.getImage() + " @ " + plan
                    + " | removedFromModel=" + removed + " | ai.size=" + hand.getAiCount());
            return;
        }

        // Safety: if we got here, we thought we had a move but couldn't place; fallback to pass.
        System.out.println("[AI] Unexpected: hadAnyPlayable but no placement succeeded; passing.");
        turnManager.next();
    }

    /**
     * Draw a single tile into the AI hand (model + UI).
     * Returns true if a tile was drawn, false if boneyard was empty.
     */
    private boolean drawOneIntoAIHand() {
        if (boneyard == null || boneyard.isEmpty()) {
            System.out.println("[AI-draw] boneyard empty");
            return false;
        }

        CDominoes drawn = boneyard.drawRandom();
        if (drawn == null) {
            System.out.println("[AI-draw] none drawn");
            return false;
        }

        // model mutation
        hand.getAiHand().add(drawn);
        System.out.println("[AI-draw] drew " + drawn + " | ai.size=" + hand.getAiCount());

        // UI node (bind to enclosing bar, not the strip)
        var url = getClass().getResource(drawn.getImage());
        if (url != null) {
            Image image = new Image(url.toExternalForm(), true);
            ImageView view = new ImageView(image);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            view.setCache(true);

            StackPane bar = (StackPane) aiStrip.getParent();
            if (bar != null) {
                view.fitHeightProperty().bind(bar.heightProperty().subtract(12));
            } else {
                Platform.runLater(() -> {
                    StackPane p = (StackPane) aiStrip.getParent();
                    if (p != null) view.fitHeightProperty().bind(p.heightProperty().subtract(12));
                });
            }
            view.setRotate(drawn.getRotationDegrees());

            StackPane hitbox = new StackPane(view);
            hitbox.setPadding(new javafx.geometry.Insets(4));
            HBox.setHgrow(hitbox, javafx.scene.layout.Priority.NEVER);

            hitbox.getProperties().put("model", drawn);
            aiStrip.getChildren().add(hitbox);
            System.out.println("[AI-draw] UI node added | children=" + aiStrip.getChildren().size());
        } else {
            System.out.println("[AI-draw] image url null (tile still added to model)");
        }

        return true;
    }
}
