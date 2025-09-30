package views;

import java.util.ArrayList;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.AvailablePieces;
import models.CDominoes;
import models.Hand;
import controllers.CPlayer;

public class CTable {
    private final Stage stage;
    private final Hand hand;
    private final AvailablePieces remainingPieces;
    private final CPlayer player;

    // boxes for players hands and table
    public final HBox aiHandStrip = new HBox(8);
    public final HBox playerHandStrip = new HBox(8);
    public final Canvas tableTop = new Canvas(800, 600);

    // heights for the three table sections
    private static final double AI_PREF = 180;
    private static final double AI_MIN  = 110;
    private static final double PLAYER_PREF = 180;
    private static final double PLAYER_MIN  = 110;
    private static final double NAVBAR_PREF = 40; 
    private static final double CENTER_MIN  = 140; 

    private final Pane overlay = new Pane();

    public CTable(Stage stage, Hand hand, AvailablePieces remainingPieces, CPlayer player) { 
        this.stage = stage; 
        this.hand = hand;
        this.remainingPieces = remainingPieces;
        this.player = player;
    }

    public Scene createScene() {
        // parent panel
        VBox root = new VBox();
        root.setPadding(Insets.EMPTY);
        root.setStyle("-fx-background-color: #151515;");

        // button to show remaining pieces
        Button remainingPiecesBtn = new Button("Remaining Pieces");
        remainingPiecesBtn.setStyle("-fx-background-color: #151515; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 1px; -fx-border-radius: 5px;"); 

        // button to quit game back to menu
        Button quitButton = new Button("Quit Game");
        quitButton.setStyle("-fx-background-color: #151515; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 1px; -fx-border-radius: 5px;"); 
        quitButton.setOnAction(e -> stage.close());

        remainingPiecesBtn.setFocusTraversable(false);
        quitButton.setFocusTraversable(false);
        
        // navbar
        HBox navbar = new HBox(remainingPiecesBtn, quitButton);
        navbar.setAlignment(Pos.CENTER_RIGHT);
        navbar.setSpacing(10);
        navbar.setPadding(new Insets(6, 6, 4, 6));
        navbar.setMinHeight(Region.USE_PREF_SIZE);
        navbar.setPrefHeight(NAVBAR_PREF);
        navbar.setMaxHeight(Region.USE_PREF_SIZE);

        // section for ai current pieces
        HBox aiHand = makeStrip(aiHandStrip, AI_MIN, AI_PREF, hand, "AI");

        // table where pieces will be played
        StackPane center = new StackPane(tableTop);
        center.setPadding(Insets.EMPTY);

        // force flex on window change
        center.widthProperty().addListener((o, ow, nw) -> tableTop.setWidth(nw.doubleValue()));
        center.heightProperty().addListener((o, oh, nh) -> tableTop.setHeight(nh.doubleValue()));
        center.setMinHeight(CENTER_MIN); 

        // human players hand
        HBox playerStrip = makeStrip(playerHandStrip, PLAYER_MIN, PLAYER_PREF, hand, "Player");

        // panel for the three sections to go inside
        BorderPane content = new BorderPane();

        // place sections accordingly
        content.setTop(aiHand);
        content.setCenter(center);
        content.setBottom(playerStrip);

        // add navbar and play area to the scene
        root.getChildren().addAll(navbar, content);

        // flex around the play area (prevents losing player hand area)
        VBox.setVgrow(content, Priority.ALWAYS);

        // overlay sits above everything
        overlay.setPickOnBounds(false);

        // wrap root and overlay together
        StackPane layeredRoot = new StackPane(root, overlay);

        // create scene
        Scene scene = new Scene(layeredRoot);

        // set focus to the whole screen
        remainingPiecesBtn.setFocusTraversable(false);
        quitButton.setFocusTraversable(false);

        scene.getRoot().requestFocus();

        // force window width
        aiHand.setMaxWidth(Double.MAX_VALUE);
        playerStrip.setMaxWidth(Double.MAX_VALUE);
        BorderPane.setAlignment(center, Pos.CENTER);

        // should additionally prevent player hand moving out of window
        double stageMinHeight = NAVBAR_PREF + AI_MIN + PLAYER_MIN + CENTER_MIN;
        stage.setMinHeight(stageMinHeight);
        stage.setMinWidth(900);

        return scene;
    }

    // turn each players hand area into a fixed-height strip (no scrolling)
    private HBox makeStrip(HBox playerHandStrip, double minimumHeight, double preferredHeight, Hand hand, String choosePlayer) {
        playerHandStrip.setAlignment(Pos.CENTER);
        playerHandStrip.setPadding(new Insets(6));
        playerHandStrip.setFillHeight(true);
        playerHandStrip.setMinHeight(minimumHeight);
        playerHandStrip.setPrefHeight(preferredHeight);
        playerHandStrip.setMaxHeight(Region.USE_PREF_SIZE);
        playerHandStrip.setStyle("-fx-background-color: #222831;");

        displayDominoes(playerHandStrip, choosePlayer);

        return playerHandStrip;
    }

    private void displayDominoes(HBox strip, String whichPlayer) {
        ArrayList<CDominoes> sideOfTable = "AI".equals(whichPlayer) ? hand.getAiHand() : hand.getPlayerHand();

        strip.getChildren().clear();

        for (CDominoes domino : sideOfTable) {
            var url = getClass().getResource(domino.getImage());
            if (url == null) {
                // could log missing asset here for debugging
                continue;
            }

            Image image = new Image(url.toExternalForm(), true);
            ImageView view = new ImageView(image);

            view.setPreserveRatio(true);
            view.setSmooth(true);
            view.setCache(true);

            view.fitHeightProperty().bind(strip.heightProperty());
            view.setRotate(domino.getRotationDegrees());

            StackPane hitbox = new StackPane(view);
            hitbox.setPadding(new Insets(4));

            if ("Player".equals(whichPlayer)) {
                // attaches drag/interaction; if you add key handlers, guard with a putIfAbsent flag
                player.definePlayerMovement(domino, overlay, hitbox, strip);
            }

            strip.getChildren().add(hitbox);
        }
    }


}
