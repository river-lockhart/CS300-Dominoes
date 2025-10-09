package views;

import java.util.ArrayList;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableNumberValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import models.AvailablePieces;
import models.CDominoes;
import models.Hand;
import controllers.CPlayer;
import controllers.TurnManager;
import views.PauseMenu;

public class CTable {
    // deps
    private final Stage stage;
    private final Hand hand;
    private final AvailablePieces remainingPieces;
    private final CPlayer player;
    private final TurnManager turnManager;

    // steady navbar height; hand bars are dynamic
    private static final double NAVBAR_H = 56;
    private static final double HANDBAR_MIN = 64;
    private static final double HANDBAR_MAX = 140;
    private static final double CENTER_MIN_H = 160;

    // domino aspect
    private static final double DOMINO_ASPECT_W_OVER_H = 0.5;

    // image resources
    private static final String HANDBAR_BG = "/assets/tabletop/playerhands.jpg";
    private static final String TABLE_BG   = "/assets/tabletop/table.jpg";

    // strips
    public final HBox aiHandStrip = new HBox(8);
    public final HBox playerHandStrip = new HBox(8);

    // play area
    public final Canvas tableTop = new Canvas(800, 600);

    // full-window layer so dragged tiles appear on top of everything
    private final Pane overlay = new Pane();

    public CTable(Stage stage, Hand hand, AvailablePieces remainingPieces, CPlayer player, TurnManager turnManager) {
        this.stage = stage;
        this.hand = hand;
        this.remainingPieces = remainingPieces;
        this.player = player;
        this.turnManager = turnManager;
    }

    // build the full layout and return the root
    public Parent createRoot() {
        AnchorPane root = new AnchorPane();
        PauseMenu pauseMenu = new PauseMenu(stage);

        root.setStyle("-fx-background-color: #151515;");

        // the main vertical box that fills the window
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

        // table
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
                            false, false, false, true) // cover
            );
            tablePane.setBackground(new Background(tBG));
        } else {
            tablePane.setStyle("-fx-background-color: #1c1f24;");
        }
        tablePane.setMinHeight(CENTER_MIN_H);
        tableTop.widthProperty().bind(tablePane.widthProperty());
        tableTop.heightProperty().bind(tablePane.heightProperty());
        tablePane.getChildren().addAll(tableTop);
        VBox.setVgrow(tablePane, Priority.ALWAYS);

        // player hand bar
        StackPane playerHandBar = buildHandBar(playerHandStrip, true);

        box.getChildren().addAll(navbar, aiHandBar, tablePane, playerHandBar);

        // configure the overlay as a full-window top layer
        overlay.setPickOnBounds(false);
        AnchorPane.setTopAnchor(overlay, 0.0);
        AnchorPane.setRightAnchor(overlay, 0.0);
        AnchorPane.setBottomAnchor(overlay, 0.0);
        AnchorPane.setLeftAnchor(overlay, 0.0);

        // add box first, overlay second so it renders above everything, and then the pause above that
        root.getChildren().addAll(box, overlay, pauseMenu.getView());
        overlay.toFront();

        AnchorPane.setTopAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setRightAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setBottomAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setLeftAnchor(pauseMenu.getView(), 0.0);

        // settings overlay above pause
        SettingsMenu settingsMenu = new SettingsMenu(stage, pauseMenu);
        root.getChildren().add(settingsMenu.getView());
        AnchorPane.setTopAnchor(settingsMenu.getView(), 0.0);
        AnchorPane.setRightAnchor(settingsMenu.getView(), 0.0);
        AnchorPane.setBottomAnchor(settingsMenu.getView(), 0.0);
        AnchorPane.setLeftAnchor(settingsMenu.getView(), 0.0);

        // swaps between pause menu and settings menu
        pauseMenu.getSettingsButton().setOnAction(e -> {
            pauseMenu.hide();
            settingsMenu.show();
        });

        // populate tiles
        displayDominoes(hand, aiHandStrip, "AI", aiHandBar);
        displayDominoes(hand, playerHandStrip, "Player", playerHandBar);

        return root;
    }

    public Scene createScene() {
        Parent root = createRoot();

        Scene scene = new Scene(root);

        stage.widthProperty().addListener((obs, oldW, newW) -> {
            System.out.println("window width: " + newW.intValue());
        });

        // ui update to indicate which player has current turn
        turnManager.turnProperty().addListener((o, oldSide, newSide) -> {
        playerHandStrip.setOpacity(newSide == TurnManager.Side.PLAYER ? 1.0 : 0.6);
        aiHandStrip.setOpacity(newSide == TurnManager.Side.AI ? 1.0 : 0.6);
        });

        stage.heightProperty().addListener((obs, oldH, newH) -> {
            System.out.println("window height: " + newH.intValue());
        });

        stage.setMinWidth(900);
        stage.setMinHeight(870);

        return scene;
    }

    private HBox buildNavbar(Stage stage, PauseMenu pauseMenu) {
        Button remainingPiecesBtn = new Button("Remaining Pieces");
        remainingPiecesBtn.setStyle("-fx-background-color: #151515; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 1px; -fx-border-radius: 6px;");
        remainingPiecesBtn.setFocusTraversable(false);

        Button menuButton = new Button("Menu");
        menuButton.setStyle("-fx-background-color: #151515; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 1px; -fx-border-radius: 6px;");
        menuButton.setFocusTraversable(false);
        menuButton.setOnAction(e -> pauseMenu.show());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, spacer, remainingPiecesBtn, menuButton);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(8, 10, 8, 10));
        bar.setStyle("-fx-background-color: #151515;");
        bar.setMinHeight(NAVBAR_H);
        bar.setPrefHeight(NAVBAR_H);
        bar.setMaxHeight(NAVBAR_H);

        bar.setMinWidth(0);
        bar.setPrefWidth(Region.USE_COMPUTED_SIZE);
        bar.setMaxWidth(Double.MAX_VALUE);

        return bar;
    }

    private StackPane buildHandBar(HBox strip, boolean alignBottom) {
        strip.setAlignment(Pos.CENTER);
        strip.setFillHeight(true);
        strip.setPadding(new Insets(6));
        strip.setSpacing(8);

        strip.setMinWidth(0);
        strip.setPrefWidth(Region.USE_COMPUTED_SIZE);
        strip.setMaxWidth(Double.MAX_VALUE);
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

        // thin white edge line at top (player) or bottom (ai)
        Rectangle edge = new Rectangle();
        edge.setManaged(false);
        edge.setMouseTransparent(true);
        edge.setFill(Color.WHITE);
        edge.setHeight(1);
        edge.widthProperty().bind(bar.widthProperty());
        if (alignBottom) {
            // player hand: edge on top
            edge.setLayoutY(0);
        } else {
            // ai hand: edge on bottom
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

            // tile height tracks bar height
            view.fitHeightProperty().bind(bar.heightProperty().subtract(12));
            view.setRotate(domino.getRotationDegrees());

            StackPane hitbox = new StackPane(view);
            hitbox.setPadding(new Insets(4));
            HBox.setHgrow(hitbox, Priority.NEVER);

            System.out.println(turnManager.turnProperty());
            if ("Player".equals(who)) {
                // disables hitbox when its not the players turn so they can't drag dominoes
                hitbox.disableProperty().bind(turnManager.turnProperty()
                    .isNotEqualTo(controllers.TurnManager.Side.PLAYER));
                player.definePlayerMovement(domino, overlay, hitbox, strip);
            }
            strip.getChildren().add(hitbox);
        }
    }
}
