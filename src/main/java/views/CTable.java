package views;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableNumberValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import models.AvailablePieces;
import models.CDominoes;
import models.Hand;
import models.TableLayout;

import controllers.CPlayer;
import controllers.AIPlayer;
import controllers.TurnManager;

public class CTable {
    private final Stage stage;
    private final Hand hand;
    private final AvailablePieces remainingPieces;
    private final CPlayer player;
    private final TurnManager turnManager;

    private AIPlayer aiPlayer;

    private static final double NAVBAR_H = 56;
    private static final double HANDBAR_MIN = 64;
    private static final double HANDBAR_MAX = 140;
    private static final double CENTER_MIN_H = 160;

    private static final double DOMINO_ASPECT_W_OVER_H = 0.5;

    private static final String HANDBAR_BG = "/assets/tabletop/playerhands.jpg";
    private static final String TABLE_BG   = "/assets/tabletop/table.jpg";

    // match navbar button look
    private static final String NAV_BTN_STYLE =
        "-fx-background-color: #151515;" +
        "-fx-text-fill: white;" +
        "-fx-border-color: white;" +
        "-fx-border-width: 1px;" +
        "-fx-border-radius: 6px;";

    public final HBox aiHandStrip = new HBox(8);
    public final HBox playerHandStrip = new HBox(8);

    // play area canvas (for size binding only)
    public final Canvas tableTop = new Canvas(800, 600);

    // top overlay where tiles live
    private final Pane overlay = new Pane();

    private TableLayout tableLayout;
    private Label turnLabel;

    // remaining pieces overlay (compact 4x2 panel)
    private AvailablePiecesOverlay<CDominoes> remainingOverlay;

    // keep a field so we can attach handler after overlay exists
    private Button remainingPiecesBtn;

    // reference to the player's hand bar StackPane so we can position the DRAW button relative to it
    private StackPane playerHandBar;

    // floating DRAW button
    private final Button drawBtn = new Button("DRAW");

    // winner overlay
    private Winner winnerOverlay;
    private boolean gameOver = false;

    public CTable(Stage stage, Hand hand, AvailablePieces remainingPieces, CPlayer player, TurnManager turnManager) {
        this.stage = stage;
        this.hand = hand;
        this.remainingPieces = remainingPieces;
        this.player = player;
        this.turnManager = turnManager;
    }

    public Parent createRoot() {
        AnchorPane root = new AnchorPane();
        PauseMenu pauseMenu = new PauseMenu(stage);

        root.setStyle("-fx-background-color: #151515;");

        VBox box = new VBox();
        box.setFillWidth(true);
        box.setSpacing(0);
        AnchorPane.setTopAnchor(box, 0.0);
        AnchorPane.setRightAnchor(box, 0.0);
        AnchorPane.setBottomAnchor(box, 0.0);
        AnchorPane.setLeftAnchor(box, 0.0);

        // navbar
        HBox navbar = buildNavbar(stage, pauseMenu);

        // ai hand bar
        StackPane aiHandBar = buildHandBar(aiHandStrip, false);

        // table pane
        StackPane tablePane = new StackPane();
        var tableUrl = getClass().getResource(TABLE_BG);
        if (tableUrl != null) {
            Image tImg = new Image(tableUrl.toExternalForm(), true);
            BackgroundImage tBG = new BackgroundImage(
                    tImg,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO,
                            false, false, false, true)
            );
            tablePane.setBackground(new Background(tBG));
        } else {
            tablePane.setStyle("-fx-background-color: #1c1f24;");
        }
        tablePane.setMinHeight(CENTER_MIN_H);

        // bind canvas to the tablePane so we always know its size
        tableTop.widthProperty().bind(tablePane.widthProperty());
        tableTop.heightProperty().bind(tablePane.heightProperty());
        tablePane.getChildren().addAll(tableTop);
        VBox.setVgrow(tablePane, Priority.ALWAYS);

        // player hand bar (keep a reference)
        playerHandBar = buildHandBar(playerHandStrip, true);

        box.getChildren().addAll(navbar, aiHandBar, tablePane, playerHandBar);

        // overlay above everything
        overlay.setPickOnBounds(false);
        AnchorPane.setTopAnchor(overlay, 0.0);
        AnchorPane.setRightAnchor(overlay, 0.0);
        AnchorPane.setBottomAnchor(overlay, 0.0);
        AnchorPane.setLeftAnchor(overlay, 0.0);

        root.getChildren().addAll(box, overlay, pauseMenu.getView());
        overlay.toFront();

        AnchorPane.setTopAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setRightAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setBottomAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setLeftAnchor(pauseMenu.getView(), 0.0);

        // Winner overlay
        winnerOverlay = new Winner(stage);
        root.getChildren().add(winnerOverlay.getView());
        AnchorPane.setTopAnchor(winnerOverlay.getView(), 0.0);
        AnchorPane.setRightAnchor(winnerOverlay.getView(), 0.0);
        AnchorPane.setBottomAnchor(winnerOverlay.getView(), 0.0);
        AnchorPane.setLeftAnchor(winnerOverlay.getView(), 0.0);

        SettingsMenu settingsMenu = new SettingsMenu(stage, pauseMenu);
        root.getChildren().add(settingsMenu.getView());
        AnchorPane.setTopAnchor(settingsMenu.getView(), 0.0);
        AnchorPane.setRightAnchor(settingsMenu.getView(), 0.0);
        AnchorPane.setBottomAnchor(settingsMenu.getView(), 0.0);
        AnchorPane.setLeftAnchor(settingsMenu.getView(), 0.0);

        pauseMenu.getSettingsButton().setOnAction(e -> {
            pauseMenu.hide();
            settingsMenu.show();
        });

        // instantiate layout AFTER overlay/tableTop exist
        tableLayout = new TableLayout(overlay, tableTop, turnManager, playerHandStrip, aiHandStrip);

        // seed the initial "+"
        tablePane.layoutBoundsProperty().addListener((o, oldB, newB) -> Platform.runLater(tableLayout::forceReseedCenterIfEmpty));
        Platform.runLater(tableLayout::forceReseedCenterIfEmpty);

        // AI controller
        aiPlayer = new AIPlayer(tableLayout, turnManager, aiHandStrip, hand, remainingPieces);

        // populate tiles
        displayDominoes(hand, aiHandStrip, "AI", aiHandBar);
        displayDominoes(hand, playerHandStrip, "Player", playerHandBar);

        // compact remaining overlay
        remainingOverlay = new AvailablePiecesOverlay<>(
                root,
                () -> remainingPieces.getLeftoverDominoes(),
                this::renderTinyDomino
        );
        remainingOverlay.setGrid(4, 2);
        remainingOverlay.setCellSize(60, 60);

        // wire navbar button
        if (remainingPiecesBtn != null) {
            remainingPiecesBtn.setOnAction(e -> remainingOverlay.toggle());
        }

        // turn label
        StringBinding turnTextBinding = Bindings.createStringBinding(
                () -> "Turn: " + (turnManager.getTurn() == TurnManager.Side.PLAYER ? "Player" : "AI"),
                turnManager.turnProperty()
        );
        turnLabel.textProperty().bind(turnTextBinding);

        // player strip change: update draw button (win check happens on turn switch)
        playerHandStrip.getChildren().addListener(
                (ListChangeListener<Node>) change -> {
                    if (!gameOver && turnManager.getTurn() == TurnManager.Side.PLAYER) {
                        updateDrawButtonSoon();
                    }
                });

        // TURN SWITCH listener — this is "end of turn" for the side that just played
        turnManager.turnProperty().addListener((o, oldSide, newSide) -> {
            if (gameOver) return;

            // After a turn finishes (commit or pass), check win state first
            WinnerSide ws = computeWinnerWithTiebreak(oldSide);
            if (ws != WinnerSide.NONE) {
                gameOver = true;
                hideDrawButton();
                winnerOverlay.show(ws == WinnerSide.PLAYER ? "YOU WON" : "AI WON");
                return;
            }

            // Otherwise proceed as normal
            playerHandStrip.setOpacity(newSide == TurnManager.Side.PLAYER ? 1.0 : 0.6);
            aiHandStrip.setOpacity(newSide == TurnManager.Side.AI ? 1.0 : 0.6);

            if (newSide == TurnManager.Side.AI && aiPlayer != null) {
                hideDrawButton();
                aiPlayer.takeTurnWithDelay();
            } else if (newSide == TurnManager.Side.PLAYER) {
                updateDrawButtonSoon();
            }
        });

        playerHandStrip.setOpacity(turnManager.getTurn() == TurnManager.Side.PLAYER ? 1.0 : 0.6);
        aiHandStrip.setOpacity(turnManager.getTurn() == TurnManager.Side.AI ? 1.0 : 0.6);

        // DRAW button
        buildDrawButton();

        // initial win check (edge cases)
        WinnerSide initial = computeWinnerWithTiebreak(null);
        if (initial != WinnerSide.NONE) {
            gameOver = true;
            winnerOverlay.show(initial == WinnerSide.PLAYER ? "YOU WON" : "AI WON");
        } else if (turnManager.getTurn() == TurnManager.Side.AI) {
            aiPlayer.takeTurnWithDelay();
        } else {
            updateDrawButtonSoon();
        }

        return root;
    }

    public Scene createScene() {
        Parent root = createRoot();
        Scene scene = new Scene(root);
        stage.setMinWidth(900);
        stage.setMinHeight(870);
        return scene;
    }

    private HBox buildNavbar(Stage stage, PauseMenu pauseMenu) {
        turnLabel = new Label("Turn: —");
        turnLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        remainingPiecesBtn = new Button("Remaining Pieces");
        remainingPiecesBtn.setStyle(NAV_BTN_STYLE);
        remainingPiecesBtn.setFocusTraversable(false);

        Button menuButton = new Button("Menu");
        menuButton.setStyle(NAV_BTN_STYLE);
        menuButton.setFocusTraversable(false);
        menuButton.setOnAction(e -> pauseMenu.show());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, turnLabel, spacer, remainingPiecesBtn, menuButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 10, 8, 10));
        bar.setStyle("-fx-background-color: #151515;");
        bar.setMinHeight(NAVBAR_H);
        bar.setPrefHeight(NAVBAR_H);
        bar.setMaxHeight(NAVBAR_H);
        return bar;
    }

    private StackPane buildHandBar(HBox strip, boolean alignBottom) {
        strip.setAlignment(Pos.CENTER);
        strip.setFillHeight(true);
        strip.setPadding(new Insets(6));
        strip.setSpacing(8);
        HBox.setHgrow(strip, Priority.ALWAYS);

        StackPane bar = new StackPane(strip);
        bar.setPadding(Insets.EMPTY);

        var bgUrl = getClass().getResource(HANDBAR_BG);
        if (bgUrl != null) {
            Image bgImg = new Image(bgUrl.toExternalForm(), true);
            BackgroundImage bgi = new BackgroundImage(
                    bgImg,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    new BackgroundPosition(Side.LEFT, 0.5, true, alignBottom ? Side.BOTTOM : Side.TOP, 0, false),
                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true)
            );
            bar.setBackground(new Background(bgi));
        } else {
            bar.setStyle("-fx-background-color: #222831;");
        }

        bar.setMinHeight(HANDBAR_MIN);
        bar.setMaxHeight(HANDBAR_MAX);
        StackPane.setAlignment(strip, Pos.CENTER);

        Rectangle edge = new Rectangle();
        edge.setManaged(false);
        edge.setMouseTransparent(true);
        edge.setFill(Color.WHITE);
        edge.setHeight(1);
        edge.widthProperty().bind(bar.widthProperty());
        if (alignBottom) {
            edge.setLayoutY(0);
        } else {
            edge.layoutYProperty().bind(bar.heightProperty().subtract(1));
        }
        bar.getChildren().add(edge);
        edge.toFront();

        final double childHPad = 8;
        final double stripSidePad = 12;
        final double spacing = strip.getSpacing();

        ObservableNumberValue tileCount = Bindings.size(strip.getChildren());
        DoubleBinding handBarPrefH = Bindings.createDoubleBinding(() -> {
            int n = Math.max(1, tileCount.intValue());
            double barW = Math.max(bar.getWidth(), 1);
            double usedByGaps = (n - 1) * spacing + (n * childHPad) + stripSidePad;
            double usableW = Math.max(0, barW - usedByGaps);
            double perTileW = usableW / n;

            double heightFromWidth = (DOMINO_ASPECT_W_OVER_H > 0)
                    ? (perTileW / DOMINO_ASPECT_W_OVER_H) + 12
                    : HANDBAR_MIN;

            return Math.max(HANDBAR_MIN, Math.min(HANDBAR_MAX, heightFromWidth));
        }, bar.widthProperty(), tileCount);

        bar.prefHeightProperty().bind(handBarPrefH);
        return bar;
    }

    private void displayDominoes(Hand hand, HBox strip, String who, StackPane bar) {
        ArrayList<CDominoes> sideOfTable = "AI".equals(who) ? hand.getAiHand() : hand.getPlayerHand();
        strip.getChildren().clear();

        for (CDominoes domino : sideOfTable) {
            var url = getClass().getResource(domino.getImage());
            if (url == null) continue;

            Image image = new Image(url.toExternalForm(), true);
            ImageView view = new ImageView(image);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            view.setCache(true);

            view.fitHeightProperty().bind(bar.heightProperty().subtract(12));
            view.setRotate(domino.getRotationDegrees());

            StackPane hitbox = new StackPane(view);
            hitbox.setPadding(new Insets(4));
            HBox.setHgrow(hitbox, Priority.NEVER);

            hitbox.getProperties().put("model", domino);

            if ("Player".equals(who)) {
                hitbox.disableProperty().bind(turnManager.turnProperty()
                        .isNotEqualTo(controllers.TurnManager.Side.PLAYER));

                hitbox.getProperties().put("onCommit", (Consumer<CDominoes>) (CDominoes d) -> {
                    hand.getPlayerHand().remove(d);
                    Platform.runLater(this::updateDrawButtonVisibility);
                });

                player.definePlayerMovement(domino, overlay, hitbox, strip, tableLayout);
            }

            strip.getChildren().add(hitbox);
        }
    }

    private Node renderTinyDomino(CDominoes domino) {
        var url = getClass().getResource(domino.getImage());
        ImageView iv = new ImageView(url != null ? new Image(url.toExternalForm(), true) : null);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setCache(true);
        iv.setRotate(domino.getRotationDegrees());
        iv.setFitHeight(54);
        return new StackPane(iv);
    }

    /* ---------------- Floating DRAW button ---------------- */

    private void buildDrawButton() {
        drawBtn.setVisible(false);
        drawBtn.setManaged(false);
        drawBtn.setPickOnBounds(true);
        drawBtn.setFocusTraversable(false);

        drawBtn.setStyle(
            NAV_BTN_STYLE +
            "-fx-font-size: 20px;" +
            "-fx-padding: 14 22 14 22;"
        );

        drawBtn.setOnAction(e -> {
            if (gameOver) return;
            if (turnManager.getTurn() == TurnManager.Side.PLAYER) {
                CDominoes d = drawOneIntoPlayerHand();
                updateDrawButtonVisibility();
            }
        });

        overlay.getChildren().add(drawBtn);

        overlay.layoutBoundsProperty().addListener((o, a, b) -> positionDrawButton());
        if (playerHandBar != null) {
            playerHandBar.layoutBoundsProperty().addListener((o, a, b) -> positionDrawButton());
            playerHandBar.localToSceneTransformProperty().addListener((o, a, b) -> positionDrawButton());
        }
    }

    private void positionDrawButton() {
        if (!drawBtn.isVisible() || playerHandBar == null) return;

        Point2D barTopLeftScene = playerHandBar.localToScene(0, 0);
        Point2D barTopLeftOverlay = (barTopLeftScene != null)
                ? overlay.sceneToLocal(barTopLeftScene)
                : new Point2D(0, 0);

        double bx = barTopLeftOverlay.getX();
        double by = barTopLeftOverlay.getY();
        double bw = playerHandBar.getWidth();
        double bh = playerHandBar.getHeight();

        double margin = 12;
        double x = Math.max(0, bx + bw - drawBtn.getWidth() - margin);
        double y = Math.max(0, by + (bh - drawBtn.getHeight()) / 2.0);

        drawBtn.relocate(x, y);
    }

    private void showDrawButton() {
        if (!overlay.getChildren().contains(drawBtn)) overlay.getChildren().add(drawBtn);
        drawBtn.setVisible(true);
        drawBtn.applyCss();
        drawBtn.autosize();
        positionDrawButton();
    }

    private void hideDrawButton() { drawBtn.setVisible(false); }

    private void updateDrawButtonSoon() {
        PauseTransition pt = new PauseTransition(Duration.millis(100));
        pt.setOnFinished(e -> updateDrawButtonVisibility());
        pt.play();
    }

    private void updateDrawButtonVisibility() {
        if (gameOver || turnManager.getTurn() != TurnManager.Side.PLAYER) {
            hideDrawButton();
            return;
        }

        tableLayout.forceReseedCenterIfEmpty();

        boolean emptyHand = hand.getPlayerHand().isEmpty();
        boolean playable = playerHasPlayableStrict();
        boolean canDraw = !remainingPieces.isEmpty();

        if ((emptyHand || !playable) && canDraw) showDrawButton();
        else hideDrawButton();
    }

    private boolean playerHasPlayableStrict() {
        if (hand.getPlayerHand().isEmpty()) return false;

        boolean anyPlayable = false;
        for (CDominoes d : hand.getPlayerHand()) {
            String orig = d.getOrientation();
            var opt = tableLayout.findLegalPlacementAnywhere(d);
            int guard = 0;
            while (!Objects.equals(d.getOrientation(), orig) && guard++ < 4) {
                CDominoes.rotateDomino(d);
            }
            if (opt.isPresent()) anyPlayable = true;
        }
        return anyPlayable;
    }

    private CDominoes drawOneIntoPlayerHand() {
        CDominoes drawn = remainingPieces.drawRandom();
        if (drawn == null) return null;

        hand.getPlayerHand().add(drawn);

        var url = getClass().getResource(drawn.getImage());
        if (url != null) {
            Image image = new Image(url.toExternalForm(), true);
            ImageView view = new ImageView(image);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            view.setCache(true);

            StackPane bar = (StackPane) playerHandStrip.getParent();
            if (bar != null) {
                view.fitHeightProperty().bind(bar.heightProperty().subtract(12));
            } else {
                Platform.runLater(() -> {
                    StackPane p = (StackPane) playerHandStrip.getParent();
                    if (p != null) view.fitHeightProperty().bind(p.heightProperty().subtract(12));
                });
            }

            view.setRotate(drawn.getRotationDegrees());

            StackPane hitbox = new StackPane(view);
            hitbox.setPadding(new Insets(4));
            HBox.setHgrow(hitbox, Priority.NEVER);

            hitbox.getProperties().put("model", drawn);
            hitbox.getProperties().put("onCommit", (Consumer<CDominoes>) (CDominoes d) -> {
                hand.getPlayerHand().remove(d);
                Platform.runLater(this::updateDrawButtonVisibility);
            });

            hitbox.disableProperty().bind(turnManager.turnProperty()
                    .isNotEqualTo(controllers.TurnManager.Side.PLAYER));

            player.definePlayerMovement(drawn, overlay, hitbox, playerHandStrip, tableLayout);
            playerHandStrip.getChildren().add(hitbox);
        } else {
            var maybeBar = (StackPane) playerHandStrip.getParent();
            displayDominoes(hand, playerHandStrip, "Player", maybeBar != null ? maybeBar : new StackPane());
        }

        if (remainingOverlay != null) remainingOverlay.refreshIfShowing();
        return drawn;
    }

    /* ----------------------- WIN STATE ----------------------- */

    private enum WinnerSide { PLAYER, AI, NONE }

    /**
     * Win states:
     * Player wins if:
     *  - player hand = 0 AND ai hand = 0 AND boneyard = 1
     *    OR
     *  - player hand = 0 AND boneyard = 0
     *
     * AI wins if:
     *  - ai hand = 0 AND player hand = 0 AND boneyard = 1
     *    OR
     *  - ai hand = 0 AND boneyard = 0
     *
     * If both conditions are simultaneously true (e.g., both 0 and boneyard 0),
     * tie-break goes to the side that just played (oldSide).
     */
    private WinnerSide computeWinnerWithTiebreak(TurnManager.Side oldSide) {
        int p = hand.getPlayerCount();
        int a = hand.getAiCount();
        int r = remainingPieces.size();

        boolean playerWins = (p == 0 && ((a == 0 && r == 1) || r == 0));
        boolean aiWins     = (a == 0 && ((p == 0 && r == 1) || r == 0));

        if (playerWins && !aiWins) return WinnerSide.PLAYER;
        if (aiWins && !playerWins) return WinnerSide.AI;

        if (playerWins && aiWins) {
            if (oldSide == TurnManager.Side.PLAYER) return WinnerSide.PLAYER;
            if (oldSide == TurnManager.Side.AI)     return WinnerSide.AI;
        }
        return WinnerSide.NONE;
    }
}
