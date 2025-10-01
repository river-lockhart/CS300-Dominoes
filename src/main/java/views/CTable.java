package views;

import java.util.ArrayList;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableNumberValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.AvailablePieces;
import models.CDominoes;
import models.Hand;
import controllers.CPlayer;
import views.PauseMenu;
import views.MainMenu;

public class CTable {
    // deps
    private final Stage stage;
    private final Hand hand;
    private final AvailablePieces remainingPieces;
    private final CPlayer player;

    // steady navbar height; hand bars are dynamic
    private static final double NAVBAR_H = 56;
    private static final double HANDBAR_MIN = 64;
    private static final double HANDBAR_MAX = 140;
    private static final double CENTER_MIN_H = 160;

    // canonical domino aspect
    private static final double DOMINO_ASPECT_W_OVER_H = 0.5;

    // strips
    public final HBox aiHandStrip = new HBox(8);
    public final HBox playerHandStrip = new HBox(8);

    // play area
    public final Canvas tableTop = new Canvas(800, 600);

    // global overlay: full-window layer so dragged tiles appear on top of everything
    private final Pane overlay = new Pane();

    public CTable(Stage stage, Hand hand, AvailablePieces remainingPieces, CPlayer player) {
        this.stage = stage;
        this.hand = hand;
        this.remainingPieces = remainingPieces;
        this.player = player;
    }

    public Scene createScene() {
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

        // 1) navbar
        HBox navbar = buildNavbar(stage, pauseMenu);

        // 2) ai hand bar
        StackPane aiHandBar = buildHandBar(aiHandStrip);

        // 3) table
        StackPane tablePane = new StackPane();
        tablePane.setStyle("-fx-background-color: #1c1f24;");
        tablePane.setMinHeight(CENTER_MIN_H);
        tableTop.widthProperty().bind(tablePane.widthProperty());
        tableTop.heightProperty().bind(tablePane.heightProperty());
        tablePane.getChildren().addAll(tableTop);
        VBox.setVgrow(tablePane, Priority.ALWAYS);

        // 4) player hand bar
        StackPane playerHandBar = buildHandBar(playerHandStrip);

        box.getChildren().addAll(navbar, aiHandBar, tablePane, playerHandBar);

        // configure the overlay as a full-window top layer
        overlay.setPickOnBounds(false); // empty areas don't block clicks
        AnchorPane.setTopAnchor(overlay, 0.0);
        AnchorPane.setRightAnchor(overlay, 0.0);
        AnchorPane.setBottomAnchor(overlay, 0.0);
        AnchorPane.setLeftAnchor(overlay, 0.0);

        // add box first, overlay second so it renders above everything, and then the pause above that
        root.getChildren().addAll(box, overlay, pauseMenu.getView());
        overlay.toFront();

        // make pause overlay fill window
        AnchorPane.setTopAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setRightAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setBottomAnchor(pauseMenu.getView(), 0.0);
        AnchorPane.setLeftAnchor(pauseMenu.getView(), 0.0);

        // ✅ wire Reset in the pause menu to return to a fresh MainMenu
        pauseMenu.getResetButton().setOnAction(e -> {
            pauseMenu.hide();
            // normalize window, then swap to clean main menu
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(870);
            stage.setWidth(1280);
            stage.setHeight(800);
            stage.centerOnScreen();
            stage.setScene(new MainMenu(stage).createScene());
        });

        // populate tiles
        displayDominoes(hand, aiHandStrip, "AI", aiHandBar);
        displayDominoes(hand, playerHandStrip, "Player", playerHandBar);

        // scene
        Scene scene = new Scene(root);

        // print window size whenever resized (handy for tuning)
        stage.widthProperty().addListener((obs, oldW, newW) -> {
            System.out.println("window width: " + newW.intValue());
        });
        stage.heightProperty().addListener((obs, oldH, newH) -> {
            System.out.println("window height: " + newH.intValue());
        });

        // min sizes
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

    private StackPane buildHandBar(HBox strip) {
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
        bar.setStyle("-fx-background-color: #222831;");
        bar.setMinHeight(HANDBAR_MIN);
        bar.setMaxHeight(HANDBAR_MAX);
        StackPane.setAlignment(strip, Pos.CENTER);

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

            // pass the global overlay so dragging re-parents to the top layer
            if ("Player".equals(who)) {
                player.definePlayerMovement(domino, overlay, hitbox, strip);
            }
            strip.getChildren().add(hitbox);
        }
    }
}
