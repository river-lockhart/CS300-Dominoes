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
import util.ConsoleLogger;

public class CTable {
    private final Stage stage;
    private final Hand hand;
    private final AvailablePieces remainingPieces;
    private final CPlayer player;
    private final TurnManager turnManager;

    private AIPlayer aiPlayer;

    private static final double NAVBAR_HEIGHT = 56;
    private static final double HAND_BAR_MIN = 64;
    private static final double HAND_BAR_MAX = 140;
    private static final double MIN_CENTER_HEIGHT = 160;

    private static final double TILE_ASPECT_RATIO = 0.5;

    private static final String HAND_BAR_IMAGE = "/assets/tabletop/playerhands.jpg";
    private static final String TABLE_IMAGE = "/assets/tabletop/table.jpg";

    private static final String NAV_BUTTON_STYLE =
        "-fx-background-color: #151515;" +
        "-fx-text-fill: white;" +
        "-fx-border-color: white;" +
        "-fx-border-width: 1px;" +
        "-fx-border-radius: 6px;";

    public final HBox computerStrip = new HBox(8);
    public final HBox playerStrip = new HBox(8);

    public final Canvas tableCanvas = new Canvas(800, 600);

    private final Pane overlay = new Pane();

    private TableLayout tableLayout;
    private Label turnLabel;

    private AvailablePiecesOverlay<CDominoes> remainingOverlay;

    private Button remainingButton;

    private StackPane playerHandBar;

    private final Button drawButton = new Button("DRAW");

    private Winner winnerOverlay;
    private boolean gameOver = false;

    // sets up references for the game table
    public CTable(Stage stage, Hand hand, AvailablePieces remainingPieces, CPlayer player, TurnManager turnManager) {
        this.stage = stage;
        this.hand = hand;
        this.remainingPieces = remainingPieces;
        this.player = player;
        this.turnManager = turnManager;
    }

    // creates the full table view layout
    public Parent createRoot() {
        // starts a fresh console log
        ConsoleLogger.startGame();

        // makes root container and pause overlay
        AnchorPane root = new AnchorPane();
        PauseMenu pauseMenu = new PauseMenu(stage);

        // sets dark app background
        root.setStyle("-fx-background-color: #151515;");

        // builds main vertical layout
        VBox container = new VBox();
        container.setFillWidth(true);
        container.setSpacing(0);
        AnchorPane.setTopAnchor(container, 0.0);
        AnchorPane.setRightAnchor(container, 0.0);
        AnchorPane.setBottomAnchor(container, 0.0);
        AnchorPane.setLeftAnchor(container, 0.0);

        // builds top navbar with controls
        HBox navbar = buildNavbar(stage, pauseMenu);

        // builds ai hand bar
        StackPane aiHandBar = buildHandBar(computerStrip, false);

        // builds play area with background
        StackPane tablePane = new StackPane();
        var tableImageUrl = getClass().getResource(TABLE_IMAGE);
        if (tableImageUrl != null) {
            Image tableImage = new Image(tableImageUrl.toExternalForm(), true);
            BackgroundImage tableBackground = new BackgroundImage(
                    tableImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO,
                            false, false, false, true)
            );
            tablePane.setBackground(new Background(tableBackground));
        } else {
            tablePane.setStyle("-fx-background-color: #1c1f24;");
        }
        tablePane.setMinHeight(MIN_CENTER_HEIGHT);

        // binds canvas to play area size
        tableCanvas.widthProperty().bind(tablePane.widthProperty());
        tableCanvas.heightProperty().bind(tablePane.heightProperty());
        tablePane.getChildren().addAll(tableCanvas);
        VBox.setVgrow(tablePane, Priority.ALWAYS);

        // builds player hand bar
        playerHandBar = buildHandBar(playerStrip, true);

        // mounts major rows into container
        container.getChildren().addAll(navbar, aiHandBar, tablePane, playerHandBar);

        // adds top overlay for tiles and buttons
        overlay.setPickOnBounds(false);
        AnchorPane.setTopAnchor(overlay, 0.0);
        AnchorPane.setRightAnchor(overlay, 0.0);
        AnchorPane.setBottomAnchor(overlay, 0.0);
        AnchorPane.setLeftAnchor(overlay, 0.0);

        // assembles root layers
        root.getChildren().addAll(container, overlay, pauseMenu.getView());
        overlay.toFront();

        // anchors pause menu to edges
        AnchorPane.setTopAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setRightAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setBottomAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setLeftAnchor(pauseMenu.getView(), 0.0);

        // mounts winner overlay
        winnerOverlay = new Winner(stage);
        root.getChildren().add(winnerOverlay.getView());
        AnchorPane.setTopAnchor(winnerOverlay.getView(), 0.0);
        AnchorPane.setRightAnchor(winnerOverlay.getView(), 0.0);
        AnchorPane.setBottomAnchor(winnerOverlay.getView(), 0.0);
        AnchorPane.setLeftAnchor(winnerOverlay.getView(), 0.0);

        // mounts settings overlay
        SettingsMenu settingsMenu = new SettingsMenu(stage, pauseMenu);
        root.getChildren().add(settingsMenu.getView());
        AnchorPane.setTopAnchor(settingsMenu.getView(), 0.0);
        AnchorPane.setRightAnchor(settingsMenu.getView(), 0.0);
        AnchorPane.setBottomAnchor(settingsMenu.getView(), 0.0);
        AnchorPane.setLeftAnchor(settingsMenu.getView(), 0.0);

        // opens settings from pause menu
        pauseMenu.getSettingsButton().setOnAction(e -> {
            pauseMenu.hide();
            settingsMenu.show();
        });

        // builds table layout engine
        tableLayout = new TableLayout(overlay, tableCanvas, turnManager, playerStrip, computerStrip);

        // seeds center join point if empty
        tablePane.layoutBoundsProperty().addListener((o, oldBounds, newBounds) -> Platform.runLater(tableLayout::forceReseedCenterIfEmpty));
        Platform.runLater(tableLayout::forceReseedCenterIfEmpty);

        // builds ai controller
        aiPlayer = new AIPlayer(tableLayout, turnManager, computerStrip, hand, remainingPieces);

        // renders dominoes for both hands
        displayDominoes(hand, computerStrip, "AI", aiHandBar);
        displayDominoes(hand, playerStrip, "Player", playerHandBar);

        // builds remaining pieces overlay
        remainingOverlay = new AvailablePiecesOverlay<>(
                root,
                () -> remainingPieces.getLeftoverDominoes(),
                this::renderTinyDomino
        );
        remainingOverlay.setGrid(4, 2);
        remainingOverlay.setCellSize(60, 60);

        // wires navbar button to overlay
        if (remainingButton != null) {
            remainingButton.setOnAction(e -> remainingOverlay.toggle());
        }

        // binds label to current turn
        StringBinding turnTextBinding = Bindings.createStringBinding(
                () -> "Turn: " + (turnManager.getTurn() == TurnManager.Side.PLAYER ? "Player" : "AI"),
                turnManager.turnProperty()
        );
        turnLabel.textProperty().bind(turnTextBinding);

        // updates draw button when hand changes
        playerStrip.getChildren().addListener(
                (ListChangeListener<Node>) change -> {
                    if (!gameOver && turnManager.getTurn() == TurnManager.Side.PLAYER) {
                        updateDrawButtonSoon();
                    }
                });

        // reacts to turn changes and drives flow
        turnManager.turnProperty().addListener((o, oldSide, newSide) -> {
            if (gameOver) return;

            // --- Instant win when boneyard is empty and someone is at 0 ---
            if (remainingPieces.size() == 0) {
                if (hand.getAiCount() == 0) {
                    gameOver = true;
                    hideDrawButton();
                    ConsoleLogger.clearCarryover();
                    ConsoleLogger.logFinalResult("Computer", "Player", hand.getPlayerHand());
                    winnerOverlay.show("AI WON");
                    return;
                }
                if (hand.getPlayerCount() == 0) {
                    gameOver = true;
                    hideDrawButton();
                    ConsoleLogger.clearCarryover();
                    ConsoleLogger.logFinalResult("Player", "Computer", hand.getAiHand());
                    winnerOverlay.show("YOU WON");
                    return;
                }
            }
            // --- End instant win ---

            // --- Single-tile endgame guard (no lookahead) ---
            int by = remainingPieces.size();
            if (by == 1) {
                if (newSide == TurnManager.Side.PLAYER) {
                    boolean playerBlocked = !playerHasPlayableStrict();
                    if (playerBlocked && hand.getAiCount() == 0) {
                        gameOver = true;
                        hideDrawButton();
                        ConsoleLogger.setCarryoverForRunnerUp(remainingPieces.getLeftoverDominoes().get(0));
                        ConsoleLogger.logFinalResult("Computer", "Player", hand.getPlayerHand());
                        winnerOverlay.show("AI WON");
                        return;
                    }
                } else if (newSide == TurnManager.Side.AI) {
                    boolean aiBlocked = !aiHasPlayableStrict();
                    if (aiBlocked && hand.getPlayerCount() == 0) {
                        gameOver = true;
                        hideDrawButton();
                        ConsoleLogger.setCarryoverForRunnerUp(remainingPieces.getLeftoverDominoes().get(0));
                        ConsoleLogger.logFinalResult("Player", "Computer", hand.getAiHand());
                        winnerOverlay.show("YOU WON");
                        return;
                    }
                }
            }
            // --- End single-tile guard ---

            // checks winner right after a turn (standard rules + tie-break)
            WinnerSide winnerSide = computeWinnerWithTiebreak(oldSide);
            if (winnerSide != WinnerSide.NONE) {
                gameOver = true;
                hideDrawButton();

                // include carryover boneyard tile if needed
                stashCarryoverIfNeededForRunnerUp();

                // logs the final required summary
                String winnerName = winnerSide == WinnerSide.PLAYER ? "Player" : "Computer";
                String runnerName = winnerSide == WinnerSide.PLAYER ? "Computer" : "Player";
                List<CDominoes> runnerTiles = winnerSide == WinnerSide.PLAYER ? hand.getAiHand() : hand.getPlayerHand();
                ConsoleLogger.logFinalResult(winnerName, runnerName, runnerTiles);

                winnerOverlay.show(winnerSide == WinnerSide.PLAYER ? "YOU WON" : "AI WON");
                return;
            }

            // dims the inactive hand
            playerStrip.setOpacity(newSide == TurnManager.Side.PLAYER ? 1.0 : 0.6);
            computerStrip.setOpacity(newSide == TurnManager.Side.AI ? 1.0 : 0.6);

            // starts ai or shows draw option
            if (newSide == TurnManager.Side.AI && aiPlayer != null) {
                hideDrawButton();
                aiPlayer.takeTurnWithDelay();
            } else if (newSide == TurnManager.Side.PLAYER) {
                updateDrawButtonSoon();
            }
        });

        // sets initial hand opacity
        playerStrip.setOpacity(turnManager.getTurn() == TurnManager.Side.PLAYER ? 1.0 : 0.6);
        computerStrip.setOpacity(turnManager.getTurn() == TurnManager.Side.AI ? 1.0 : 0.6);

        // builds floating draw button
        buildDrawButton();

        // --- Instant win check on startup (resume/edge cases) ---
        if (remainingPieces.size() == 0) {
            if (hand.getAiCount() == 0) {
                gameOver = true;
                ConsoleLogger.clearCarryover();
                ConsoleLogger.logFinalResult("Computer", "Player", hand.getPlayerHand());
                winnerOverlay.show("AI WON");
                return root;
            }
            if (hand.getPlayerCount() == 0) {
                gameOver = true;
                ConsoleLogger.clearCarryover();
                ConsoleLogger.logFinalResult("Player", "Computer", hand.getAiHand());
                winnerOverlay.show("YOU WON");
                return root;
            }
        }
        // --- End instant win on startup ---

        // handles edge case wins and first mover
        WinnerSide initialWinner = computeWinnerWithTiebreak(null);
        if (initialWinner != WinnerSide.NONE) {
            gameOver = true;

            // include carryover boneyard tile if needed
            stashCarryoverIfNeededForRunnerUp();

            // logs the final required summary
            String winnerName = initialWinner == WinnerSide.PLAYER ? "Player" : "Computer";
            String runnerName = initialWinner == WinnerSide.PLAYER ? "Computer" : "Player";
            List<CDominoes> runnerTiles = initialWinner == WinnerSide.PLAYER ? hand.getAiHand() : hand.getPlayerHand();
            ConsoleLogger.logFinalResult(winnerName, runnerName, runnerTiles);

            winnerOverlay.show(initialWinner == WinnerSide.PLAYER ? "YOU WON" : "AI WON");
        } else if (turnManager.getTurn() == TurnManager.Side.AI) {
            aiPlayer.takeTurnWithDelay();
        } else {
            updateDrawButtonSoon();
        }

        return root;
    }

    // creates a new scene for the table
    public Scene createScene() {
        Parent root = createRoot();
        Scene scene = new Scene(root);
        stage.setMinWidth(900);
        stage.setMinHeight(870);
        return scene;
    }

    // builds the top navbar with controls
    private HBox buildNavbar(Stage stage, PauseMenu pauseMenu) {
        turnLabel = new Label("Turn: —");
        turnLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        remainingButton = new Button("Remaining Pieces");
        remainingButton.setStyle(NAV_BUTTON_STYLE);
        remainingButton.setFocusTraversable(false);

        Button menuButton = new Button("Menu");
        menuButton.setStyle(NAV_BUTTON_STYLE);
        menuButton.setFocusTraversable(false);
        menuButton.setOnAction(e -> pauseMenu.show());

        Region growSpacer = new Region();
        HBox.setHgrow(growSpacer, Priority.ALWAYS);

        HBox bar = new HBox(10, turnLabel, growSpacer, remainingButton, menuButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 10, 8, 10));
        bar.setStyle("-fx-background-color: #151515;");
        bar.setMinHeight(NAVBAR_HEIGHT);
        bar.setPrefHeight(NAVBAR_HEIGHT);
        bar.setMaxHeight(NAVBAR_HEIGHT);
        return bar;
    }

    // builds a hand bar with dynamic height
    // sets height using simple width math
    private StackPane buildHandBar(HBox strip, boolean alignBottom) {
        strip.setAlignment(Pos.CENTER);
        strip.setFillHeight(true);
        strip.setPadding(new Insets(6));
        strip.setSpacing(8);
        HBox.setHgrow(strip, Priority.ALWAYS);

        StackPane bar = new StackPane(strip);
        bar.setPadding(Insets.EMPTY);

        var handImageUrl = getClass().getResource(HAND_BAR_IMAGE);
        if (handImageUrl != null) {
            Image backgroundImage = new Image(handImageUrl.toExternalForm(), true);
            BackgroundImage handBackgroundImage = new BackgroundImage(
                    backgroundImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    new BackgroundPosition(Side.LEFT, 0.5, true, alignBottom ? Side.BOTTOM : Side.TOP, 0, false),
                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true)
            );
            bar.setBackground(new Background(handBackgroundImage));
        } else {
            bar.setStyle("-fx-background-color: #222831;");
        }

        bar.setMinHeight(HAND_BAR_MIN);
        bar.setMaxHeight(HAND_BAR_MAX);
        StackPane.setAlignment(strip, Pos.CENTER);

        Rectangle edgeLine = new Rectangle();
        edgeLine.setManaged(false);
        edgeLine.setMouseTransparent(true);
        edgeLine.setFill(Color.WHITE);
        edgeLine.setHeight(1);
        edgeLine.widthProperty().bind(bar.widthProperty());
        if (alignBottom) {
            edgeLine.setLayoutY(0);
        } else {
            edgeLine.layoutYProperty().bind(bar.heightProperty().subtract(1));
        }
        bar.getChildren().add(edgeLine);
        edgeLine.toFront();

        // computes tile height from available width
        final double childPad = 8;
        final double stripSidePad = 12;
        final double spacing = strip.getSpacing();

        ObservableNumberValue tileCountObs = Bindings.size(strip.getChildren());
        DoubleBinding handPrefHeight = Bindings.createDoubleBinding(() -> {
            int count = Math.max(1, tileCountObs.intValue());
            double barWidth = Math.max(1, bar.getWidth());
            double usedByGaps = (count - 1) * spacing + (count * childPad) + stripSidePad;
            double usableWidth = Math.max(0, barWidth - usedByGaps);
            double perTileWidth = usableWidth / count;

            double heightFromWidth = (TILE_ASPECT_RATIO > 0)
                    ? (perTileWidth / TILE_ASPECT_RATIO) + 12
                    : HAND_BAR_MIN;

            return Math.max(HAND_BAR_MIN, Math.min(HAND_BAR_MAX, heightFromWidth));
        }, bar.widthProperty(), tileCountObs);

        bar.prefHeightProperty().bind(handPrefHeight);
        return bar;
    }

    // renders tiles into the given strip
    private void displayDominoes(Hand hand, HBox strip, String sideName, StackPane bar) {
        ArrayList<CDominoes> sideList = "AI".equals(sideName) ? hand.getAiHand() : hand.getPlayerHand();
        strip.getChildren().clear();

        for (CDominoes tile : sideList) {
            var imageUrl = getClass().getResource(tile.getImage());
            if (imageUrl == null) continue;

            Image image = new Image(imageUrl.toExternalForm(), true);
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setCache(true);

            // keeps tile height tied to bar height
            imageView.fitHeightProperty().bind(bar.heightProperty().subtract(12));
            imageView.setRotate(tile.getRotationDegrees());

            StackPane hitbox = new StackPane(imageView);
            hitbox.setPadding(new Insets(4));
            HBox.setHgrow(hitbox, Priority.NEVER);

            hitbox.getProperties().put("model", tile);

            if ("Player".equals(sideName)) {
                // disables when not the player turn
                hitbox.disableProperty().bind(turnManager.turnProperty()
                        .isNotEqualTo(controllers.TurnManager.Side.PLAYER));

                // removes tile from model on commit
                hitbox.getProperties().put("onCommit", (Consumer<CDominoes>) (CDominoes placed) -> {
                    hand.getPlayerHand().remove(placed);
                    Platform.runLater(this::updateDrawButtonVisibility);
                });

                player.definePlayerMovement(tile, overlay, hitbox, strip, tableLayout);
            }

            strip.getChildren().add(hitbox);
        }
    }

    // renders a tiny domino for the overlay
    private Node renderTinyDomino(CDominoes tile) {
        var imageUrl = getClass().getResource(tile.getImage());
        ImageView imageView = new ImageView(imageUrl != null ? new Image(imageUrl.toExternalForm(), true) : null);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.setRotate(tile.getRotationDegrees());
        imageView.setFitHeight(54);
        return new StackPane(imageView);
    }

    // builds the floating draw button
    private void buildDrawButton() {
        drawButton.setVisible(false);
        drawButton.setManaged(false);
        drawButton.setPickOnBounds(true);
        drawButton.setFocusTraversable(false);

        drawButton.setStyle(
            NAV_BUTTON_STYLE +
            "-fx-font-size: 20px;" +
            "-fx-padding: 14 22 14 22;"
        );

        drawButton.setOnAction(e -> {
            // draws only during player turn
            if (gameOver) return;
            if (turnManager.getTurn() == TurnManager.Side.PLAYER) {
                CDominoes drawnTile = drawOneIntoPlayerHand();

                // logs the draw to console
                if (drawnTile != null) {
                    ConsoleLogger.logDraw(TurnManager.Side.PLAYER, drawnTile);
                }

                updateDrawButtonVisibility();
            }
        });

        overlay.getChildren().add(drawButton);

        // repositions when sizes change
        overlay.layoutBoundsProperty().addListener((o, a, b) -> positionDrawButton());
        if (playerHandBar != null) {
            playerHandBar.layoutBoundsProperty().addListener((o, a, b) -> positionDrawButton());
            playerHandBar.localToSceneTransformProperty().addListener((o, a, b) -> positionDrawButton());
        }
    }

    // positions the draw button near the hand
    // uses scene to local conversion
    private void positionDrawButton() {
        if (!drawButton.isVisible() || playerHandBar == null) return;

        Point2D handTopLeftScene = playerHandBar.localToScene(0, 0);
        Point2D handTopLeftOverlay = (handTopLeftScene != null)
                ? overlay.sceneToLocal(handTopLeftScene)
                : new Point2D(0, 0);

        double handX = handTopLeftOverlay.getX();
        double handY = handTopLeftOverlay.getY();
        double handWidth = playerHandBar.getWidth();
        double handHeight = playerHandBar.getHeight();

        // centers vertically and hugs right edge
        double margin = 12;
        double placeX = Math.max(0, handX + handWidth - drawButton.getWidth() - margin);
        double placeY = Math.max(0, handY + (handHeight - drawButton.getHeight()) / 2.0);

        drawButton.relocate(placeX, placeY);
    }

    // shows the draw button
    private void showDrawButton() {
        if (!overlay.getChildren().contains(drawButton)) overlay.getChildren().add(drawButton);
        drawButton.setVisible(true);
        drawButton.applyCss();
        drawButton.autosize();
        positionDrawButton();
    }

    // hides the draw button
    private void hideDrawButton() { drawButton.setVisible(false); }

    // schedules a short delay before updating
    private void updateDrawButtonSoon() {
        PauseTransition pauseTimer = new PauseTransition(Duration.millis(100));
        pauseTimer.setOnFinished(e -> updateDrawButtonVisibility());
        pauseTimer.play();
    }

    // updates draw button based on state
    private void updateDrawButtonVisibility() {
        if (gameOver || turnManager.getTurn() != TurnManager.Side.PLAYER) {
            hideDrawButton();
            return;
        }

        // ensures the center starter exists
        tableLayout.forceReseedCenterIfEmpty();

        boolean emptyHand = hand.getPlayerHand().isEmpty();
        boolean hasPlayable = playerHasPlayableStrict();
        boolean canDrawTile = !remainingPieces.isEmpty();

        if ((emptyHand || !hasPlayable) && canDrawTile) showDrawButton();
        else hideDrawButton();
    }

    // checks if any player tile is playable
    // restores tile facing after probing
    private boolean playerHasPlayableStrict() {
        if (hand.getPlayerHand().isEmpty()) return false;

        boolean anyPlayable = false;
        for (CDominoes tile : hand.getPlayerHand()) {
            String startFacing = tile.getOrientation();
            var placementOption = tableLayout.findLegalPlacementAnywhere(tile);
            int rotateGuard = 0;
            while (!Objects.equals(tile.getOrientation(), startFacing) && rotateGuard++ < 4) {
                CDominoes.rotateDomino(tile);
            }
            if (placementOption.isPresent()) anyPlayable = true;
        }
        return anyPlayable;
    }

    // checks if any AI tile is playable (mirror helper)
    private boolean aiHasPlayableStrict() {
        if (hand.getAiHand().isEmpty()) return false;

        boolean anyPlayable = false;
        for (CDominoes tile : hand.getAiHand()) {
            String startFacing = tile.getOrientation();
            var placementOption = tableLayout.findLegalPlacementAnywhere(tile);
            int rotateGuard = 0;
            while (!Objects.equals(tile.getOrientation(), startFacing) && rotateGuard++ < 4) {
                CDominoes.rotateDomino(tile);
            }
            if (placementOption.isPresent()) anyPlayable = true;
        }
        return anyPlayable;
    }

    // draws one tile for the player hand
    private CDominoes drawOneIntoPlayerHand() {
        CDominoes drawnTile = remainingPieces.drawRandom();
        if (drawnTile == null) return null;

        hand.getPlayerHand().add(drawnTile);

        var imageUrl = getClass().getResource(drawnTile.getImage());
        if (imageUrl != null) {
            Image image = new Image(imageUrl.toExternalForm(), true);
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setCache(true);

            // binds tile height to hand bar
            StackPane bar = (StackPane) playerStrip.getParent();
            if (bar != null) {
                imageView.fitHeightProperty().bind(bar.heightProperty().subtract(12));
            } else {
                Platform.runLater(() -> {
                    StackPane p = (StackPane) playerStrip.getParent();
                    if (p != null) imageView.fitHeightProperty().bind(p.heightProperty().subtract(12));
                });
            }

            imageView.setRotate(drawnTile.getRotationDegrees());

            StackPane hitbox = new StackPane(imageView);
            hitbox.setPadding(new Insets(4));
            HBox.setHgrow(hitbox, Priority.NEVER);

            hitbox.getProperties().put("model", drawnTile);
            hitbox.getProperties().put("onCommit", (Consumer<CDominoes>) (CDominoes placed) -> {
                hand.getPlayerHand().remove(placed);
                Platform.runLater(this::updateDrawButtonVisibility);
            });

            hitbox.disableProperty().bind(turnManager.turnProperty()
                    .isNotEqualTo(controllers.TurnManager.Side.PLAYER));

            player.definePlayerMovement(drawnTile, overlay, hitbox, playerStrip, tableLayout);
            playerStrip.getChildren().add(hitbox);
        } else {
            var maybeBar = (StackPane) playerStrip.getParent();
            displayDominoes(hand, playerStrip, "Player", maybeBar != null ? maybeBar : new StackPane());
        }

        if (remainingOverlay != null) remainingOverlay.refreshIfShowing();
        return drawnTile;
    }

    // simple winner states for player and ai
    private enum WinnerSide { PLAYER, AI, NONE }

    // computes winner and breaks ties by last mover
    private WinnerSide computeWinnerWithTiebreak(TurnManager.Side lastTurnSide) {
        int playerCount = hand.getPlayerCount();
        int computerCount = hand.getAiCount();
        int boneyardCount = remainingPieces.size();

        boolean playerWins = (playerCount == 0 && ((computerCount == 0 && boneyardCount == 1) || boneyardCount == 0));
        boolean computerWins = (computerCount == 0 && ((playerCount == 0 && boneyardCount == 1) || boneyardCount == 0));

        if (playerWins && !computerWins) return WinnerSide.PLAYER;
        if (computerWins && !playerWins) return WinnerSide.AI;

        if (playerWins && computerWins) {
            if (lastTurnSide == TurnManager.Side.PLAYER) return WinnerSide.PLAYER;
            if (lastTurnSide == TurnManager.Side.AI)     return WinnerSide.AI;
        }
        return WinnerSide.NONE;
    }

    // sets carryover tile if exactly one remains
    private void stashCarryoverIfNeededForRunnerUp() {
        List<CDominoes> leftover = remainingPieces.getLeftoverDominoes();
        if (leftover != null && leftover.size() == 1) {
            ConsoleLogger.setCarryoverForRunnerUp(leftover.get(0));
        } else {
            ConsoleLogger.clearCarryover();
        }
    }
}
